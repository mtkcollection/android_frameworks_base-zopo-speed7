/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <sys/sysinfo.h>
#if defined(HAVE_PTHREADS)
#include <sys/resource.h>
#endif

#include "Debug.h"
#include "TaskManager.h"
#include "Task.h"
#include "TaskProcessor.h"
#include "utils/MathUtils.h"

namespace android {
namespace uirenderer {

///////////////////////////////////////////////////////////////////////////////
// Manager
///////////////////////////////////////////////////////////////////////////////

TaskManager::TaskManager() {
    // Get the number of available CPUs. This value does not change over time.
    int cpuCount = sysconf(_SC_NPROCESSORS_CONF);

    int workerCount = MathUtils::max(1, cpuCount / 2);
    ALOGD("TaskManager() %p, cpu = %d, thread = %d", this, cpuCount, workerCount);
    for (int i = 0; i < workerCount; i++) {
        String8 name;
        name.appendFormat("hwuiTask%d", i + 1);
        mThreads.add(new WorkerThread(name));
    }
}

TaskManager::~TaskManager() {
    ALOGD("~TaskManager() %p", this);
    for (size_t i = 0; i < mThreads.size(); i++) {
        mThreads[i]->exit();
    }
}

bool TaskManager::canRunTasks() const {
    return mThreads.size() > 0;
}

void TaskManager::stop() {
    ALOGD("[TaskMgr] %p stop", this);
    for (size_t i = 0; i < mThreads.size(); i++) {
        mThreads[i]->exit();
    }
}

bool TaskManager::addTaskBase(const sp<TaskBase>& task, const sp<TaskProcessorBase>& processor) {
    if (mThreads.size() > 0) {
        TaskWrapper wrapper(task, processor);

        size_t minQueueSize = INT_MAX;
        sp<WorkerThread> thread;

        for (size_t i = 0; i < mThreads.size(); i++) {
            if (mThreads[i]->getTaskCount() < minQueueSize) {
                thread = mThreads[i];
                minQueueSize = mThreads[i]->getTaskCount();
            }
        }

        return thread->addTask(wrapper);
    }
    return false;
}

///////////////////////////////////////////////////////////////////////////////
// Thread
///////////////////////////////////////////////////////////////////////////////

status_t TaskManager::WorkerThread::readyToRun() {
#if defined(HAVE_PTHREADS)
    setpriority(PRIO_PROCESS, 0, PRIORITY_FOREGROUND);
#endif
    return NO_ERROR;
}

bool TaskManager::WorkerThread::threadLoop() {
    mSignal.wait();
    Vector<TaskWrapper> tasks;
    {
        Mutex::Autolock l(mLock);
        tasks = mTasks;
        mTasks.clear();
    }

    for (size_t i = 0; i < tasks.size(); i++) {
        const TaskWrapper& task = tasks.itemAt(i);
        task.mProcessor->process(task.mTask);
        if (g_HWUI_debug_shadow || g_HWUI_debug_paths) {
            ALOGD("[TaskMgr] %s finish task", mName.string());
        }
    }

    return true;
}

bool TaskManager::WorkerThread::addTask(TaskWrapper task) {
    /// M: force exiting thread before new task coming
    ///    if the thread is going to die
    if (isRunning() && exitPending()) {
#if defined(HAVE_PTHREADS) && defined(HAVE_ANDROID_OS)
        ALOGW("[TaskMgr] waiting for %s (%d) exiting", mName.string(), getTid());
#endif
        requestExitAndWait();
    }

    if (!isRunning()) {
        run(mName.string(), PRIORITY_DEFAULT);
#if defined(HAVE_PTHREADS) && defined(HAVE_ANDROID_OS)
        ALOGD("[TaskMgr] Running thread %s (%d)", mName.string(), getTid());
#endif
    }
    if (g_HWUI_debug_shadow || g_HWUI_debug_paths) {
        ALOGD("[TaskMgr] Add task to %s", mName.string());
    }

    Mutex::Autolock l(mLock);

#if defined(HAVE_PTHREADS) && defined(HAVE_ANDROID_OS)
    /// M: store the tid to task
    task.mTask->mTid = getTid();
#endif

    ssize_t index = mTasks.add(task);
    mSignal.signal();

    return index >= 0;
}

size_t TaskManager::WorkerThread::getTaskCount() const {
    Mutex::Autolock l(mLock);
    return mTasks.size();
}

void TaskManager::WorkerThread::exit() {
#if defined(HAVE_PTHREADS) && defined(HAVE_ANDROID_OS)
    ALOGD("[TaskMgr] Exit thread %s (%d)", mName.string(), getTid());
#endif
    {
        Mutex::Autolock l(mLock);
        mTasks.clear();
    }
    requestExit();
    mSignal.signal();
}

}; // namespace uirenderer
}; // namespace android

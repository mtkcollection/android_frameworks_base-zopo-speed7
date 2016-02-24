/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

#include "RenderThread.h"

#if defined(HAVE_PTHREADS)
#include <sys/resource.h>
#endif
#include <gui/DisplayEventReceiver.h>
#include <utils/Log.h>

#include "../RenderState.h"
#include "CanvasContext.h"
#include "EglManager.h"
#include "RenderProxy.h"

namespace android {
using namespace uirenderer::renderthread;
ANDROID_SINGLETON_STATIC_INSTANCE(RenderThread);

namespace uirenderer {
namespace renderthread {

#if defined(MTK_DEBUG_RENDERER)
    #define RENDER_THREAD_LOGD(...) if (g_HWUI_debug_render_thread) ALOGD(__VA_ARGS__)
#else
    #define RENDER_THREAD_LOGD(...)
#endif

// Number of events to read at a time from the DisplayEventReceiver pipe.
// The value should be large enough that we can quickly drain the pipe
// using just a few large reads.
static const size_t EVENT_BUFFER_SIZE = 100;

// Slight delay to give the UI time to push us a new frame before we replay
static const nsecs_t DISPATCH_FRAME_CALLBACKS_DELAY = milliseconds_to_nanoseconds(4);

/// M: Helper class, track when the RenderTask post
class TaskHolder : public RenderTask {
public:
    static void* sLastTask; // can be used as log only
    static char sLastTaskName[30];
    static nsecs_t sLastTaskPostAt;
    static nsecs_t sLastTaskRunFrom;
    static nsecs_t sLastTaskRunTo;

    TaskHolder(RenderTask* task) : mTask(task) {
        mRunAt = task->mRunAt;
    }
    virtual ~TaskHolder() { }

    virtual void run() {
#if defined(MTK_DEBUG_RENDERER)
        sLastTask = mTask;
        strcpy(sLastTaskName, mTask->name());
        sLastTaskPostAt = mPostAt;
        sLastTaskRunFrom = systemTime(SYSTEM_TIME_MONOTONIC);
        sLastTaskRunTo = 0;
        mPostAt = 0;
        RENDER_THREAD_LOGD("task (%p, %s) post at %" PRId64 ", run at %" PRId64 ,
            mTask, sLastTaskName, sLastTaskPostAt, sLastTaskRunFrom);
#endif
        mTask->run();
#if defined(MTK_DEBUG_RENDERER)
        sLastTaskRunTo = systemTime(SYSTEM_TIME_MONOTONIC);
        int runMillis = nanoseconds_to_milliseconds(sLastTaskRunTo - sLastTaskRunFrom);

        // log task's address only, it may have deleted itself
        if (sLastTaskRunTo - sLastTaskRunFrom > g_HWUI_debug_anr_ns) {
            ALOGD("[ANR Warning] task (%p, %s) post at %" PRId64 ", run from %" PRId64 " to %" PRId64 " (%dms)",
                mTask, sLastTaskName, sLastTaskPostAt, sLastTaskRunFrom, sLastTaskRunTo, runMillis);
        } else {
            RENDER_THREAD_LOGD("task (%p, %s) post at %" PRId64 ", run from %" PRId64 " to %" PRId64 " (%dms)",
                mTask, sLastTaskName, sLastTaskPostAt, sLastTaskRunFrom, sLastTaskRunTo, runMillis);
        }
#endif
        // task is done, suicide here to release holder
        delete this;
    }

    virtual const char* name() {
        return mTask->name();
    }

    inline void attach(TaskHolder* taskHolder) {
        mNext = taskHolder;
        mTask->mNext = taskHolder ? taskHolder->mTask : NULL;
    }

    inline void detach() {
        mNext = NULL;
        mTask->mNext = NULL;
    }

    inline TaskHolder* next() {
        return reinterpret_cast<TaskHolder*>(mNext);
    }

public:
    RenderTask* mTask;
    nsecs_t mPostAt;
};

void* TaskHolder::sLastTask = NULL;
char TaskHolder::sLastTaskName[30];
nsecs_t TaskHolder::sLastTaskPostAt = 0;
nsecs_t TaskHolder::sLastTaskRunFrom = 0;
nsecs_t TaskHolder::sLastTaskRunTo = 0;

TaskQueue::TaskQueue() : mHead(0), mTail(0) {}

RenderTask* TaskQueue::next(bool kill) {
    TaskHolder* holder = reinterpret_cast<TaskHolder*>(mHead);
    if (holder) {
        mHead = holder->next();
        if (!mHead) {
            mTail = NULL;
        }
        holder->detach();
    }

    if (kill) {
        delete holder;
        holder = NULL;
    }

    return holder;
}

RenderTask* TaskQueue::peek() {
    return mHead ? reinterpret_cast<TaskHolder*>(mHead)->mTask : NULL;
}

void TaskQueue::queue(RenderTask* task) {
    // Since the RenderTask itself forms the linked list it is not allowed
    // to have the same task queued twice
    LOG_ALWAYS_FATAL_IF(task->mNext ||
            (mTail != NULL && reinterpret_cast<TaskHolder*>(mTail)->mTask == task),
            "Task is already in the queue!");

    TaskHolder* holder = new TaskHolder(task);
    if (mTail) {
        // Fast path if we can just append
        if (mTail->mRunAt <= task->mRunAt) {
            reinterpret_cast<TaskHolder*>(mTail)->attach(holder);
            mTail = holder;
        } else {
            // Need to find the proper insertion point
            TaskHolder* previous = NULL;
            TaskHolder* next = reinterpret_cast<TaskHolder*>(mHead);
            while (next && next->mRunAt <= task->mRunAt) {
                previous = next;
                next = next->next();
            }
            if (!previous) {
                holder->attach(reinterpret_cast<TaskHolder*>(mHead));
                mHead = holder;
            } else {
                previous->attach(holder);
                if (next) {
                    holder->attach(next);
                } else {
                    mTail = holder;
                }
            }
        }
    } else {
        mTail = mHead = holder;
    }

#if defined(MTK_DEBUG_RENDERER)
    holder->mPostAt = systemTime(SYSTEM_TIME_MONOTONIC);
    RENDER_THREAD_LOGD("task (%p, %s) post at %" PRId64, holder->mTask, holder->name(), holder->mPostAt);
#endif
}

void TaskQueue::queueAtFront(RenderTask* task) {
    TaskHolder* holder = new TaskHolder(task);
    if (mTail) {
        holder->attach(reinterpret_cast<TaskHolder*>(mHead));
        mHead = holder;
    } else {
        mTail = mHead = holder;
    }
#if defined(MTK_DEBUG_RENDERER)
    holder->mPostAt = systemTime(SYSTEM_TIME_MONOTONIC);
    RENDER_THREAD_LOGD("task (%p, %s) post at %" PRId64, holder->mTask, holder->name(), holder->mPostAt);
#endif
}

void TaskQueue::remove(RenderTask* task) {
    // TaskQueue is strict here to enforce that users are keeping track of
    // their RenderTasks due to how their memory is managed
    LOG_ALWAYS_FATAL_IF(!task->mNext &&
            (mTail != NULL && reinterpret_cast<TaskHolder*>(mTail)->mTask != task),
            "Cannot remove a task that isn't in the queue!");

    // If task is the head we can just call next() to pop it off
    // Otherwise we need to scan through to find the task before it
    if (peek() == task) {
        next(true);
    } else {
        TaskHolder* previous = reinterpret_cast<TaskHolder*>(mHead);
        while (previous->next()->mTask != task) {
            previous = previous->next();
        }
        TaskHolder* holder = previous->next();
        previous->attach(holder->next());
        if (mTail == holder) {
            mTail = previous;
        }
        delete holder;
    }
}

void TaskQueue::dump(String8& log, int timeoutNs) {
    nsecs_t current = systemTime(SYSTEM_TIME_MONOTONIC);
    // dump if over timeout and there are pending tasks or the last task is not finshed yet
    if ((current - TaskHolder::sLastTaskRunFrom > timeoutNs) &&
        (mHead || TaskHolder::sLastTaskRunTo <= 0)) {
        log.appendFormat("TaskQueue:\n");

        if (mHead) {
            TaskHolder* holder = reinterpret_cast<TaskHolder*>(mHead);
            while(holder) {
                log.appendFormat("  (%p, %s) post at %" PRId64 ", wait in %" PRId64 "ms\n",
                    holder->mTask, holder->name(), holder->mPostAt,
                    nanoseconds_to_milliseconds(current - holder->mPostAt));
                holder = holder->next();
            }
        } else {
            log.appendFormat("  empty\n");
        }

        log.appendFormat("Last task:\n");
        if (TaskHolder::sLastTaskRunTo > 0)
            current = TaskHolder::sLastTaskRunTo;
        log.appendFormat("  (%p, %s) post at %" PRId64 ", run from %" PRId64 " to %" PRId64 " (%" PRId64 "ms)%s\n",
            TaskHolder::sLastTask, TaskHolder::sLastTaskName,
            TaskHolder::sLastTaskPostAt, TaskHolder::sLastTaskRunFrom, current,
            nanoseconds_to_milliseconds(current - TaskHolder::sLastTaskRunFrom),
            TaskHolder::sLastTaskRunTo > 0 ? " ": " but not finished yet!!");
    }
}

class DispatchFrameCallbacks : public RenderTask {
private:
    RenderThread* mRenderThread;
public:
    DispatchFrameCallbacks(RenderThread* rt) : mRenderThread(rt) {}

    virtual void run() {
        mRenderThread->dispatchFrameCallbacks();
    }

    virtual const char* name() {
        return "DispatchFrameCallbacks";
    }
};

RenderThread::RenderThread() : Thread(true), Singleton<RenderThread>()
        , mNextWakeup(LLONG_MAX)
        , mDisplayEventReceiver(0)
        , mVsyncRequested(false)
        , mFrameCallbackTaskPending(false)
        , mFrameCallbackTask(0)
        , mRenderState(NULL)
        , mEglManager(NULL) {
    setDebugLog(); /// M: enable log in the beginning
    mFrameCallbackTask = new DispatchFrameCallbacks(this);
    mLooper = new Looper(false);
    run("RenderThread");
}

RenderThread::~RenderThread() {
    LOG_ALWAYS_FATAL("Can't destroy the render thread");
}

void RenderThread::initializeDisplayEventReceiver() {
    LOG_ALWAYS_FATAL_IF(mDisplayEventReceiver, "Initializing a second DisplayEventReceiver?");
    mDisplayEventReceiver = new DisplayEventReceiver();
    ALOGD("initialize DisplayEventReceiver %p", mDisplayEventReceiver);
    status_t status = mDisplayEventReceiver->initCheck();
    LOG_ALWAYS_FATAL_IF(status != NO_ERROR, "Initialization of DisplayEventReceiver "
            "failed with status: %d", status);

    // Register the FD
    mLooper->addFd(mDisplayEventReceiver->getFd(), 0,
            Looper::EVENT_INPUT, RenderThread::displayEventReceiverCallback, this);
}

void RenderThread::initThreadLocals() {
    initializeDisplayEventReceiver();
    mEglManager = new EglManager(*this);
    mRenderState = new RenderState(*this);
}

int RenderThread::displayEventReceiverCallback(int fd, int events, void* data) {
    if (events & (Looper::EVENT_ERROR | Looper::EVENT_HANGUP)) {
        ALOGE("Display event receiver pipe was closed or an error occurred.  "
                "events=0x%x", events);
        return 0; // remove the callback
    }

    if (!(events & Looper::EVENT_INPUT)) {
        ALOGW("Received spurious callback for unhandled poll event.  "
                "events=0x%x", events);
        return 1; // keep the callback
    }

    ATRACE_CALL_L2();
    reinterpret_cast<RenderThread*>(data)->drainDisplayEventQueue();

    return 1; // keep the callback
}

static nsecs_t latestVsyncEvent(DisplayEventReceiver* receiver) {
    DisplayEventReceiver::Event buf[EVENT_BUFFER_SIZE];
    nsecs_t latest = 0;
    ssize_t n;
    while ((n = receiver->getEvents(buf, EVENT_BUFFER_SIZE)) > 0) {
        for (ssize_t i = 0; i < n; i++) {
            const DisplayEventReceiver::Event& ev = buf[i];
            switch (ev.header.type) {
            case DisplayEventReceiver::DISPLAY_EVENT_VSYNC:
                latest = ev.header.timestamp;
                break;
            }
        }
    }
    if (n < 0) {
        ALOGW("Failed to get events from display event receiver, status=%d", status_t(n));
    }
    return latest;
}

void RenderThread::drainDisplayEventQueue() {
    ATRACE_CALL();
    nsecs_t vsyncEvent = latestVsyncEvent(mDisplayEventReceiver);
    if (vsyncEvent > 0) {
        mVsyncRequested = false;
        if (mTimeLord.vsyncReceived(vsyncEvent) && !mFrameCallbackTaskPending) {
            ATRACE_NAME("queue mFrameCallbackTask");
            mFrameCallbackTaskPending = true;
            nsecs_t runAt = (vsyncEvent + DISPATCH_FRAME_CALLBACKS_DELAY);
            queueAt(mFrameCallbackTask, runAt);
        }
    }
}

void RenderThread::dispatchFrameCallbacks() {
    ATRACE_CALL();
    mFrameCallbackTaskPending = false;

    std::set<IFrameCallback*> callbacks;
    mFrameCallbacks.swap(callbacks);

    if (callbacks.size()) {
        // Assume one of them will probably animate again so preemptively
        // request the next vsync in case it occurs mid-frame
        requestVsync();
        for (std::set<IFrameCallback*>::iterator it = callbacks.begin(); it != callbacks.end(); it++) {
            (*it)->doFrame();
        }
    }
}

void RenderThread::requestVsync() {
    if (!mVsyncRequested) {
        ATRACE_CALL_L2();
        mVsyncRequested = true;
        status_t status = mDisplayEventReceiver->requestNextVsync();
        LOG_ALWAYS_FATAL_IF(status != NO_ERROR,
                "requestNextVsync failed with status: %d", status);
    }
}

bool RenderThread::threadLoop() {
#if defined(HAVE_PTHREADS)
    setpriority(PRIO_PROCESS, 0, PRIORITY_DISPLAY);
#endif
    initThreadLocals();

    int timeoutMillis = -1;
    for (;;) {
        int result = mLooper->pollOnce(timeoutMillis);
        LOG_ALWAYS_FATAL_IF(result == Looper::POLL_ERROR,
                "RenderThread Looper POLL_ERROR!");

        nsecs_t nextWakeup;
        // Process our queue, if we have anything
        while (RenderTask* task = nextTask(&nextWakeup)) {
            task->run();
            // task may have deleted itself, do not reference it again
        }
        if (nextWakeup == LLONG_MAX) {
            timeoutMillis = -1;
        } else {
            nsecs_t timeoutNanos = nextWakeup - systemTime(SYSTEM_TIME_MONOTONIC);
            timeoutMillis = nanoseconds_to_milliseconds(timeoutNanos);
            if (timeoutMillis < 0) {
                timeoutMillis = 0;
            }
        }

        if (mPendingRegistrationFrameCallbacks.size() && !mFrameCallbackTaskPending) {
            drainDisplayEventQueue();
            mFrameCallbacks.insert(
                    mPendingRegistrationFrameCallbacks.begin(), mPendingRegistrationFrameCallbacks.end());
            mPendingRegistrationFrameCallbacks.clear();
            requestVsync();
        }

        if (!mFrameCallbackTaskPending && !mVsyncRequested && mFrameCallbacks.size()) {
            // TODO: Clean this up. This is working around an issue where a combination
            // of bad timing and slow drawing can result in dropping a stale vsync
            // on the floor (correct!) but fails to schedule to listen for the
            // next vsync (oops), so none of the callbacks are run.
            requestVsync();
        }
    }

    return false;
}

void RenderThread::queue(RenderTask* task) {
    AutoMutex _lock(mLock);
    mQueue.queue(task);
    if (mNextWakeup && task->mRunAt < mNextWakeup) {
        mNextWakeup = 0;
        mLooper->wake();
    }
}

void RenderThread::queueAtFront(RenderTask* task) {
    AutoMutex _lock(mLock);
    mQueue.queueAtFront(task);
    mLooper->wake();
}

void RenderThread::queueAt(RenderTask* task, nsecs_t runAtNs) {
    task->mRunAt = runAtNs;
    queue(task);
}

void RenderThread::remove(RenderTask* task) {
    AutoMutex _lock(mLock);
    mQueue.remove(task);
}

void RenderThread::postFrameCallback(IFrameCallback* callback) {
    mPendingRegistrationFrameCallbacks.insert(callback);
}

bool RenderThread::removeFrameCallback(IFrameCallback* callback) {
    size_t erased;
    erased = mFrameCallbacks.erase(callback);
    erased |= mPendingRegistrationFrameCallbacks.erase(callback);
    return erased;
}

void RenderThread::pushBackFrameCallback(IFrameCallback* callback) {
    if (mFrameCallbacks.erase(callback)) {
        mPendingRegistrationFrameCallbacks.insert(callback);
    }
}

RenderTask* RenderThread::nextTask(nsecs_t* nextWakeup) {
    AutoMutex _lock(mLock);
    RenderTask* next = mQueue.peek();
    if (!next) {
        mNextWakeup = LLONG_MAX;
    } else {
        mNextWakeup = next->mRunAt;
        // Most tasks won't be delayed, so avoid unnecessary systemTime() calls
        if (next->mRunAt <= 0 || next->mRunAt <= systemTime(SYSTEM_TIME_MONOTONIC)) {
            next = mQueue.next();
        } else {
            next = 0;
        }
    }
    if (nextWakeup) {
        *nextWakeup = mNextWakeup;
    }
    return next;
}

/// M: dump unhandled tasks in the queue
void RenderThread::dumpTaskQueue(String8& log, int timeoutNs) {
    AutoMutex _lock(mLock);
    mQueue.dump(log, timeoutNs);
}

} /* namespace renderthread */
} /* namespace uirenderer */
} /* namespace android */

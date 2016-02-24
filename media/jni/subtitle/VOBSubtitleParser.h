//#ifdef MTK_SUBTITLE_SUPPORT

#ifndef TIMED_TEXT_VOB_SUBTITLE_PARSER_H_
#define TIMED_TEXT_VOB_SUBTITLE_PARSER_H_

#include <sys/mman.h>
#include <fcntl.h>

#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaSource.h>
#include <utils/Compat.h>  // off64_t

namespace android
{

typedef unsigned int UINT32;
typedef unsigned char UINT8;


#define PIXEL_SIZE (4)

#define FILE_BUFFER_SIZE (1024 * 768 * PIXEL_SIZE)

const int VOB_PALETTE_SIZE = 16 ;


typedef int VOB_SUB_PALETTE[VOB_PALETTE_SIZE];

#define VOB_TMP_FILE_COUNT  4
#define VOB_TMP_FILE_MASK  (VOB_TMP_FILE_COUNT - 1)


class VOBSubtitleParser
{
public:

    enum ctrl_seq_type
    {
        ctrl_seq_type_start,
        ctrl_seq_type_end,
        ctrl_seq_type_none
    };

    struct RLECode
    {
        unsigned short value;
        unsigned short size;
    };

    struct ARGB_8888_Color
    {
        unsigned char a;
        unsigned char r;
        unsigned char g;
        unsigned char b;       
    };

    enum SUB_PARSE_STATE_E
    {
        SUB_PARSE_DONE,
        SUB_PARSE_NEXTLOOP,
        SUB_PARSE_FAIL,
    };

    struct SUBTITLE_PACKET_DATA
    {
        void * m_pvSubtitlePacketBuffer;         //Data Buffer
        int m_iLength;                           //Buffer Length
        int m_iCurrentOffset;                    //Current Offset in Buffer
    };
    
    VOBSubtitleParser();
    VOBSubtitleParser(char *data, int size);
    ~VOBSubtitleParser();
    void vSetPalette(const VOB_SUB_PALETTE palette);
    status_t stParse(off64_t off64);
    status_t stParseSubtitlePacket();
    status_t stParseControlPacket();
    status_t stParseControlSequence(int & seqStartOffset);
    status_t stParseDataPacket();
    status_t stParseDataSingleRLECode(char *dataReader, bool halfByte, int &bitsRead, short &RLECode);
    status_t stParseRLECode(RLECode rleCode, int &count, int &colorIdx);
    status_t stClear();
    void vReset();
    void vReadRLECode(int & offset, bool & isNibble, RLECode & rleCode);
    status_t stInit(void *data, int size);
    status_t stPrepareBitmapBuffer();
    status_t stUnmapBitmapBuffer();
    void vUnInit();
    void    incTmpFileIdx() {mCurrTmpFileIdx++;};
    int     getTmpFileIdx() {return (mCurrTmpFileIdx & VOB_TMP_FILE_MASK);};
        
    int m_iBitmapWidth;
    int m_iBitmapHeight;

    int m_iSubWidth;
    int m_iSubHeight;

    unsigned int mCurrTmpFileIdx;
    
    int m_iBeginTime;
    int m_iEndTime;

    int m_iFd[VOB_TMP_FILE_COUNT];
    void * m_pvBitmapData[VOB_TMP_FILE_COUNT];

    int m_iDataPacketSize;

    VOB_SUB_PALETTE m_aiPalette;

    bool m_fgParseFlag;

private:
    char *m_pcBuffer;
    int m_iSize;
    //FILE *mSUBFile;
    int m_iSubtitlePacketSize;

    char m_acPalette[4];
    char m_acAlpha[4];
    char m_acColor[4];
    ARGB_8888_Color m_rColor[4];
    // Xstart, Xend, Ystart, Yend
    short m_acDisplayRange[4];
    int m_iEvenLineStart;
    int m_iOddLineStart;
    SUBTITLE_PACKET_DATA m_rSpData;
    int readBigEndian(char *pByte, int bytesCount = 2);
    void vGenerateSubColorInfo(unsigned char paletteIdx, unsigned char alpha, ARGB_8888_Color & rColor);
    unsigned char ucExtendAlphaFrom4bitTo8bit(unsigned char alpha);
    void vEvDvdspuDecodeD8888(UINT32 u4Width, 
                                                UINT32 u4Height, 
                                                UINT8 *pu1Src, 
                                                UINT32 *pu4Dst, 
                                                UINT32 *pu4Cpt, 
                                                UINT32 u4Pitch);
    void memset32(UINT32 *pu4Dst, UINT32 ui4Value, UINT32 z_l);
};

};
#endif //#ifndef TIMED_TEXT_VOB_SUBTITLE_PARSER_H_
//#endif //MTK_SUBTITLE_SUPPORT


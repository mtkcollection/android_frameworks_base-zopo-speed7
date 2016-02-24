package com.mediatek.epdg;

/**
 * Constant class for EPDG SW module.
 *
 * @hide
 */
class EpdgConstants {


    /***** Constants. *****/
    static final int APN_DEFAULT_TYPE = 0;
    static final int APN_IMS_TYPE = 1;
    static final int APN_FAST_TYPE = 2;
    static final int APN_NUM_TYPES = 3;

    static final int NETWORK_SCORE = 80;
    static final int NETWORK_LOW_SCORE = -1;

    static final int ATTACH_COMMMAND = 0;
    static final int DETACH_COMMMAND = 1;
    static final int HANDOVER_COMMMAND = 2;
    static final int HANDOVER_WIFI_COMMMAND = 3;

    static final int EPDG_RESPONSE_OK = 0;

    static final String ATTACH_DATA = "woattach";
    static final String DETACH_DATA = "wodetach";
    static final String DISCONNECT_DATA = "wodisconnect";
    static final String HANDOVER_DATA = "wohol";
    static final String HANDOVER_WIFI_DATA = "wohow";

    static final String EPDG_SERVER = "server";
    static final String EPDG_AUTH = "auth";
    static final String EPDG_SIM_INDEX = "sim_index";
    static final String EPDG_MOBILITY_PROTOCOL = "mobility_protocol";
    static final String EPDG_CERT_PATH = "cert_path";
    static final String EPDG_IKEA_ALGO = "ikea_algo";
    static final String EPDG_ESP_ALGO = "esp_algo";

    static final String EMPTY_DATA = "";
}
package com.example.nimdem;

/**
 * Created by 陈澳军 on 2019/11/19.
 */

public enum CallStateEnum {
    /**
     * 未知
     */
    UNKNOWN(-1),
    /**
     * 音频通话
     */
    AUDIO(1),
    /**
     * 视频通话
     */
    VIDEO(2),
    /**
     * 链接
     * */
    VIDEO_CONNECTING(3);

    private int value;

    CallStateEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static CallStateEnum typeOfValue(int value) {
        for (CallStateEnum e : values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return UNKNOWN;
    }
}

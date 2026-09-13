package com.bgssai.media.common.oauth;

public class OAuthUserInfo {
    private final String openId;
    private final String nickname;

    public OAuthUserInfo(String openId, String nickname) {
        this.openId = openId;
        this.nickname = nickname;
    }

    public String getOpenId() { return openId; }
    public String getNickname() { return nickname; }
}

package dot.cpp.login.models.session.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Transient;
import dot.cpp.repository.models.BaseEntity;
import play.mvc.Http;
import play.mvc.Http.Cookie;

@Entity("Session")
public class Session extends BaseEntity {

  private String userId;
  private Long refreshExpiryDate;
  private String accessToken;
  private String refreshToken;
  private Long createTime;

  @Transient @JsonIgnore private Cookie cookie;

  /**
   * PerformedLogout.
   *
   * @return boolean
   */
  public boolean performedLogout() {
    return refreshExpiryDate == null || refreshExpiryDate == 0;
  }

  /**
   * Returns "Authorisation" cookie specific to this Session.
   *
   * @return Cookie
   */
  @JsonIgnore
  public Cookie getAuthorisationCookie() {
    if (cookie == null) {
      cookie =
          Cookie.builder(Http.HeaderNames.AUTHORIZATION, accessToken)
              .withSecure(true)
              .withHttpOnly(true)
              .build();
    }
    return cookie;
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  public Long getRefreshExpiryDate() {
    return refreshExpiryDate;
  }

  public void setRefreshExpiryDate(Long refreshExpiryDate) {
    this.refreshExpiryDate = refreshExpiryDate;
  }
}

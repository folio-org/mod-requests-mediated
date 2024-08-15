package org.folio.mr.util;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.folio.spring.utils.RequestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@UtilityClass
@Log4j2
public class HttpUtils {
  private static final String ACCESS_TOKEN_COOKIE_NAME = "folioAccessToken";
  private static final String KEY_TENANT = "tenant";
  private static final String KEY_SUB = "sub";
  private static final String KEY_USER_ID = "user_id";
  private static final String TOKEN_SECTION_SEPARATOR = "\\.";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static Optional<String> getTenantFromToken() {
    return getValueFromToken(KEY_TENANT);
  }

  public static Optional<String> getUsernameFromToken() {
    return getValueFromToken(KEY_SUB);
  }

  public static Optional<String> getUserIdFromToken() {
    return getValueFromToken(KEY_USER_ID);
  }

  private static Optional<String> getValueFromToken(String key) {
    return getToken()
      .flatMap(token -> getValueFromToken(token, key));
  }

  private static Optional<String> getToken() {
    return Optional.ofNullable(RequestUtils.getHttpServletRequest())
      .flatMap(request -> getCookie(request, ACCESS_TOKEN_COOKIE_NAME));
  }

  private static Optional<String> getCookie(HttpServletRequest request, String cookieName) {
    return Optional.ofNullable(request)
      .map(HttpServletRequest::getCookies)
      .flatMap(cookies -> getCookie(cookies, cookieName))
      .map(Cookie::getValue);
  }

  private static Optional<Cookie> getCookie(Cookie[] cookies, String cookieName) {
    return Arrays.stream(cookies)
      .filter(cookie -> StringUtils.equals(cookie.getName(), cookieName))
      .findFirst();
  }

  private static Optional<String> getValueFromToken(String token, String key) {
    log.info("getValueFromToken:: extracting value for key '{}' from token", key);
    try {
      byte[] decodedPayload = Base64.getDecoder()
        .decode(token.split(TOKEN_SECTION_SEPARATOR)[1]);
      String value = OBJECT_MAPPER.readTree(decodedPayload)
        .get(key)
        .asText();

      log.info("getValueFromToken:: successfully extracted value from token: {}={}", key, value);
      return Optional.ofNullable(value);
    } catch (Exception e) {
      log.error("getValueFromToken:: failed to extract value for key `{}` from token", key, e);
      return Optional.empty();
    }
  }

}

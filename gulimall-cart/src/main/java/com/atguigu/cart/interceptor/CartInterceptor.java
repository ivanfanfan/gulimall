package com.atguigu.cart.interceptor;

import com.atguigu.common.constant.CartConstant;
import com.atguigu.common.constant.ums.AuthServerConstant;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.cart.vo.UserInfoTo;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 拦截器用来判断用户的登录状态
 * 1.建立拦截器类继承HandlerInterceptor
 */
public class CartInterceptor implements HandlerInterceptor {

    /**
     * ThreadLocal同一线程数据共享
     */
    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    /**
     * 目标方法执行之前 - 封装用户信息
     * @param request
     * @param response
     * @param handler
     * @return
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        UserInfoTo userInfoTo = new UserInfoTo();

        HttpSession session = request.getSession();
        MemberRespVo memberRespVo = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);

        if (memberRespVo != null) {
            // 用户登录了
            userInfoTo.setUserId(memberRespVo.getId());
        }

        /**
         * 判断当前浏览器中的cookie中是否存在临时用户的标识的cookie
         * 获取浏览器中的cookie
         */
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if (name.equals(CartConstant.TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    userInfoTo.setTempUser(true); // 设置辨别是否有临时用户的boolean字段
                }
            }
        }
        // 如果没有临时用户，则系统自动分配一个
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }
        // 把封装的用户信息放到ThreadLocal
        threadLocal.set(userInfoTo);
        return true;
    }

    /**
     * 目标方法执行之后 - 把分配的临时用户添加到cookie
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {

        // TODO cookie过期修改TempUser状态
        UserInfoTo userInfoTo = threadLocal.get();
        if (!userInfoTo.isTempUser()) { // 如果没有临时用户才需要添加cookie
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }

    }
}

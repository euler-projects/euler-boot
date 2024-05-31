package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.eulerframework.security.web.EulerSecurityController;
import org.eulerframework.web.core.base.controller.ThymeleafSupportWebController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class DefaultEulerSecurityController extends ThymeleafSupportWebController implements EulerSecurityController {
    private EulerBootSecurityWebProperties eulerBootSecurityWebProperties;

    @Override
    @GetMapping("${euler.security.web.login-page:/login}")
    public String loginPage() {
        return "security/web/login";
    }

    @Override
    @GetMapping("${euler.security.web.logout-page:/logout}")
    public String logoutPage() {
        return "security/web/logout";
    }

    @ModelAttribute("loginProcessingUrl")
    public String loginProcessingUrl() {
        return this.eulerBootSecurityWebProperties.getLoginProcessingUrl();
    }

    @ModelAttribute("logoutProcessingUrl")
    public String logoutProcessingUrl() {
        return this.eulerBootSecurityWebProperties.getLogoutProcessingUrl();
    }

    @Autowired
    public void setEulerBootSecurityWebProperties(EulerBootSecurityWebProperties eulerBootSecurityWebProperties) {
        this.eulerBootSecurityWebProperties = eulerBootSecurityWebProperties;
    }
}

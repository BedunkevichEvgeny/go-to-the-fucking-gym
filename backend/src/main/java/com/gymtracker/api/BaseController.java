package com.gymtracker.api;

import com.gymtracker.application.security.AuthenticationService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseController {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final AuthenticationService authenticationService;

    protected BaseController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    protected UUID extractUserId() {
        return authenticationService.getCurrentUserId();
    }
}


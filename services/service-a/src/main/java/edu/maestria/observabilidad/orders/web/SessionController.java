package edu.maestria.observabilidad.orders.web;

import edu.maestria.observabilidad.orders.domain.SessionRequest;
import edu.maestria.observabilidad.orders.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    @PostMapping("/api/session")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void open(@RequestBody(required = false) SessionRequest request) {
        sessions.open(request == null ? null : request.token());
    }
}

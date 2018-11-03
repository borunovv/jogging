package com.borunovv.jogging.web.controllers;

import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.StringUtils;
import com.borunovv.core.util.TimeUtils;
import com.borunovv.core.web.HttpController;
import com.borunovv.jogging.config.Constants;
import com.borunovv.jogging.permissions.Action;
import com.borunovv.jogging.permissions.Subject;
import com.borunovv.jogging.timings.model.Timing;
import com.borunovv.jogging.timings.model.TimingWithWeather;
import com.borunovv.jogging.timings.service.TimingService;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.users.model.User;
import com.borunovv.jogging.users.service.UserService;
import com.borunovv.jogging.web.model.AbstractRequest;
import com.borunovv.jogging.web.model.RequestWithPagination;
import com.borunovv.jogging.web.model.SuccessResponse;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@HttpController(path = "/timing/")
public class TimingController extends AuthorizedController {

    @Override
    protected Object handleRequest(String requestJson, HttpRequest request, User user) {
        ensurePOST(request);

        String command = parseCommand(request.getUriPath());
        switch (command) {
            // Url: /timing/create
            case "create":
                return doCreate(requestJson, user);

            // Url: /timing/update
            case "update":
                return doUpdate(requestJson, user);

            // Url: /timing/delete
            case "delete":
                return doDelete(requestJson, user);

            // Url: /timing/list
            case "list":
                return doList(requestJson, user);

            default:
                throw new RuntimeException("No API for path '" + request.getUriPath() + "'");
        }
    }

    private Object doCreate(String requestJson, User caller) {
        CreateRequest request = toModel(requestJson, CreateRequest.class);
        User owner = ensureHasPermission(caller, Action.CREATE, Subject.TIMINGS, request.login);

        // Empty (null) date means current date.
        Date date = request.date != null ?
                TimeUtils.parseDateTime_YYYYMMDD_GMT0(request.date) :
                new Date();

        Assert.isTrue(!StringUtils.isNullOrEmpty(request.location), "Expected location");
        Assert.notNull(request.distance, "Expected distance (in meters)");
        Assert.notNull(request.time, "Expected time (in minutes)");

        long timingId = timingService.addTiming(owner.getId(), date, request.location, request.distance, request.time);
        return new CreateResponse(timingId);
    }

    private Object doUpdate(String requestJson, User caller) {
        UpdateRequest request = toModel(requestJson, UpdateRequest.class);
        Timing timing = timingService.ensureExists(request.id);
        User owner = userService.ensureUser(timing.getUserId());
        boolean actionOnSelfAccount = caller.equals(owner);
        Rights ownerRights = actionOnSelfAccount ?
                Rights.Self :
                owner.getRights();

        ensureHasPermission(caller, Action.UPDATE, Subject.TIMINGS, ownerRights);
        if (request.date != null) {
            Date date = TimeUtils.parseDateTime_YYYYMMDD_GMT0(request.date);
            timing.setDate(date);
        }
        if (request.location != null) {
            timing.setLocation(request.location);
        }
        if (request.distanceMeters != null) {
            timing.setDistanceMeters(request.distanceMeters);
        }
        if (request.timeMinutes != null) {
            timing.setTimeMinutes(request.timeMinutes);
        }

        timingService.update(timing);
        return SuccessResponse.INSTANCE;
    }

    private Object doDelete(String requestJson, User caller) {
        DeleteRequest request = toModel(requestJson, DeleteRequest.class);
        Timing timing = timingService.ensureExists(request.id);
        User owner = userService.ensureUser(timing.getUserId());
        boolean actionOnSelfAccount = caller.equals(owner);
        Rights ownerRights = actionOnSelfAccount ?
                Rights.Self :
                owner.getRights();

        ensureHasPermission(caller, Action.DELETE, Subject.TIMINGS, ownerRights);

        timingService.delete(timing);
        return SuccessResponse.INSTANCE;
    }

    private Object doList(String requestJson, User caller) {
        ListRequest request = toModel(requestJson, ListRequest.class);
        User owner = ensureHasPermission(caller, Action.READ, Subject.TIMINGS, request.login);
        long offset = request.getOffset();
        long count = request.getCount();
        List<TimingWithWeather> timings = count > 0 ?
                timingService.findAllWithWeather(owner.getId(), request.filter, offset, count) :
                new ArrayList<>();

        return new ListResponse(timings);
    }

    private User ensureHasPermission(User caller, Action action, Subject subject, @Nullable String ownerLogin) {
        // null login means owner is caller self.
        boolean actionOnSelfAccount = (ownerLogin == null);
        User owner = actionOnSelfAccount ?
                caller :
                userService.ensureUser(ownerLogin);
        Rights ownerRights = actionOnSelfAccount ?
                Rights.Self :
                owner.getRights();
        ensureHasPermission(caller, action, subject, ownerRights);
        return owner;
    }


    static class CreateRequest extends AbstractRequest {
        // Optional, not set or null means self account
        public String login;
        // Optional, not set or null means current date
        public String date;
        public String location;
        public Long distance;
        public Long time;


        public CreateRequest() {
        }

        public CreateRequest(String session, String userLogin, String date, String location, int distanceMeters, int timeMinutes) {
            super(session);
            this.login = userLogin;
            this.date = date;
            this.location = location;
            this.distance = (long) distanceMeters;
            this.time = (long) timeMinutes;
        }

        public CreateRequest(String session, String date, String location, int distanceMeters, int timeMinutes) {
            this(session, null, date, location, distanceMeters, timeMinutes);
        }

        public CreateRequest(String session, String location, int distanceMeters, int timeMinutes) {
            this(session, null, location, distanceMeters, timeMinutes);
        }
    }

    static class CreateResponse extends SuccessResponse {
        public long id;

        public CreateResponse() {
        }

        public CreateResponse(long timingId) {
            this.id = timingId;
        }
    }

    static class UpdateRequest extends AbstractRequest {
        public long id;
        public String date;
        public String location;
        public Integer distanceMeters;
        public Integer timeMinutes;

        public UpdateRequest() {
        }

        public UpdateRequest(String session, long id, String date, String location, Integer distanceMeters, Integer timeMinutes) {
            super(session);
            this.id = id;
            this.date = date;
            this.location = location;
            this.distanceMeters = distanceMeters;
            this.timeMinutes = timeMinutes;
        }
    }

    static class DeleteRequest extends AbstractRequest {
        public long id;

        public DeleteRequest() {
        }

        public DeleteRequest(String session, long timingId) {
            super(session);
            this.id = timingId;
        }
    }

    static class ListRequest extends RequestWithPagination {
        // Optional, not set or null means self account
        public String login;
        // Optional, not set or null means no filter
        public String filter;

        public ListRequest(String session, String login, long offset, long count) {
            setSession(session);
            setOffset(offset);
            setCount(count);
        }
    }

    static class ListResponse extends SuccessResponse {
        public List<TimingWithWeather> timings;

        public ListResponse() {
        }

        public ListResponse(List<TimingWithWeather> timings) {
            this.timings = timings;
        }
    }

    @Inject
    private UserService userService;
    @Inject
    private TimingService timingService;
}

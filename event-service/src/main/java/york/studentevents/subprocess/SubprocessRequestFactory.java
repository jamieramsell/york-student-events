package york.studentevents.subprocess;

import com.google.gson.Gson;

public class SubprocessRequestFactory {
    private static final Gson GSON = new Gson();

    static class Request<T> {
        private final String type;
        private final T payload;

        public Request(String type, T payload) {
            this.type = type;
            this.payload = payload;
        }
    }

    record UserIdPayload(Long user_id) {}
    record AwardBadgePayload(Long user_id, String badge_name) {}

    private static String buildGetUserBadges(Long userId) {
        Request<UserIdPayload> request = new Request<>("GET_USER_BADGES", new UserIdPayload(userId));
        return GSON.toJson(request);
    }

    private static String buildGetUserFriends(Long userId) {
        Request<UserIdPayload> request = new Request<>("GET_USER_FRIENDS", new UserIdPayload(userId));
        return GSON.toJson(request);
    }

    private static String buildAwardBadge(Long userId, String badgeName) {
        Request<AwardBadgePayload> request = new Request<>("AWARD_BADGE", new AwardBadgePayload(userId, badgeName));
        return GSON.toJson(request);
    }
}
package sk.iway.iwcm.users;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;

/** Caches the display status of two-factor authentication without retaining its secret. */
@Service
public class TwoFactorStatusService {
    private static final String CACHE_KEY_PREFIX = "users.twoFactorStatus:";
    private static final int CACHE_SECONDS = 600;

    private final UserDetailsRepository userDetailsRepository;

    public TwoFactorStatusService(UserDetailsRepository userDetailsRepository) {
        this.userDetailsRepository = userDetailsRepository;
    }

    /**
     * Reads a cached Boolean, loading the account's status on a cache miss.
     * Authentication must read the current secret directly instead of using this display status.
     *
     * @param userId account whose status is displayed
     * @return whether the account has a configured second factor
     */
    public boolean isEnabled(long userId) {
        Cache cache = Cache.getInstance();
        String key = CACHE_KEY_PREFIX + userId;
        Boolean enabled = cache.getObject(key, Boolean.class);
        if (enabled != null) return enabled;

        synchronized (this) {
            enabled = cache.getObject(key, Boolean.class);
            if (enabled == null) {
                enabled = Tools.isNotEmpty(userDetailsRepository.getMobileDeviceByUserId(userId));
                cache.setObjectSeconds(key, enabled, CACHE_SECONDS, false);
            }
            return enabled;
        }
    }

    /**
     * Clears the account's display status locally and on the cluster after a successful setup change.
     * Synchronizing with cache misses prevents an earlier local read from restoring stale status.
     *
     * @param userId account whose second-factor setup changed
     */
    public synchronized void invalidate(long userId) {
        Cache.getInstance().removeObject(CACHE_KEY_PREFIX + userId, true);
    }
}

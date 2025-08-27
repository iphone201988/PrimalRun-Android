package com.primal.runs.utils.recyclerView;

public interface SpringSystemListener {

    /**
     * Runs before each pass through the physics integration loop providing an opportunity to do any
     * setup or alterations to the Physics state before integrating.
     * @param springSystem the BaseSpringSystem listened to
     */
    void onBeforeIntegrate(BaseSpringSystem springSystem);

    /**
     * Runs after each pass through the physics integration loop providing an opportunity to do any
     * setup or alterations to the Physics state after integrating.
     * @param springSystem the BaseSpringSystem listened to
     */
    void onAfterIntegrate(BaseSpringSystem springSystem);
}


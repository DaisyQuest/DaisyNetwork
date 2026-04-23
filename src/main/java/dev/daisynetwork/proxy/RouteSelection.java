package dev.daisynetwork.proxy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RouteSelection(
        Optional<RouteSnapshot.ServiceRoute> selectedRoute,
        List<RouteMatch> candidates,
        List<RouteConflict> conflicts) {
    public RouteSelection {
        selectedRoute = Objects.requireNonNull(selectedRoute, "selectedRoute");
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        conflicts = List.copyOf(Objects.requireNonNull(conflicts, "conflicts"));
    }

    public boolean ambiguous() {
        return !conflicts.isEmpty();
    }
}

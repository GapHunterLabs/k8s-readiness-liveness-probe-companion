# Demo data for screenshots

`deployment.yaml` — `orders-api` has no `readinessProbe:` or
`livenessProbe:` (flagged twice), `orders-worker` has both (not
flagged).

## How to get the screenshot

1. `./gradlew runIde` from `k8s-readiness-liveness-probe-companion`,
   open this `demo/` folder as the project.
2. Full Screen, open `deployment.yaml` — two warnings should appear on
   `orders-api`'s container name line but none on `orders-worker`'s.
3. Screenshot with both containers visible, save into
   `k8s-readiness-liveness-probe-companion/docs/screenshots/`. Close
   the sandbox.

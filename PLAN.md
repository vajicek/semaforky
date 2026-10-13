# Bounced Arrow Extension Plan

## Confirmed requirements

- Rename the existing **Custom Set** feature to **Bounced Arrow**.
- The operator manually marks a bounced arrow while a line is running.
- The system stops at that line's end and waits for a manual trigger.
- The bounced-arrow period is a full set: red, preparation, green, warning, and stop.
- It is available at any line boundary, including after the last line of a set.
- Replace the old variable-set-at-set-boundary behavior with this flow.

## New flow

1. During `READY`, `FIRE`, or `WARNING`, the operator taps **Bounced Arrow...**. This toggles a pending mark without starting a set.
2. When the line ends, `SET_STOPPED` checks the mark.
3. If marked, the system stays stopped, sets the semaphore to red, and opens the Bounced Arrow dialog.
4. **Start** runs a full bounced-arrow set using the configured bounced-arrow time.
5. When it ends, the normal line/set progression resumes.
6. **Skip** advances to the next line or set without running the extra period.

## Planned code changes

### `web_app/app/webapp/semaforky/src/app/settings.ts`

- Rename `customSetTime` to `bouncedArrowTime`.
- Use `bouncedArrowTime` for the persisted cookie key.
- Keep the default at 120 seconds.

### `web_app/app/webapp/semaforky/src/app/scheduler.ts`

- Rename `startCustomSet()` to `startBouncedArrow()`.
- Use `settings.bouncedArrowTime`.

### `web_app/app/webapp/semaforky/src/app/states.ts`

- Rename `CUSTOM_SET_STARTED` to `BOUNCED_ARROW_STARTED`.
- Rename the active custom-set flag to `bouncedArrowActive`.
- Add `pendingBouncedArrow`.
- Persist the new flags in cookies.
- Add methods to toggle the pending mark, query it, continue after skipping, and advance the line/set counters.
- Update `SET_STOPPED` to distinguish normal line completion, a pending bounced arrow, and completion of the inserted bounced-arrow set.
- Clear pending state when a set or round is canceled/stopped.

### `web_app/app/webapp/semaforky/src/app/main/main.component.ts`

- Add a `bouncedArrowPrompt` event to the component event bus.
- Open the dialog when the state machine reaches a marked line boundary.
- Rename custom-set UI fields and handlers.
- Enable the mark button during `READY`, `FIRE`, and `WARNING`.
- Show whether a bounced arrow is currently marked.
- Add Start and Skip handlers.

### `web_app/app/webapp/semaforky/src/app/main/main.component.html`

- Rename the button and dialog from Custom Set to Bounced Arrow.
- Bind the dialog time input to `settings.bouncedArrowTime`.
- Add a Skip action.
- Visually indicate a pending mark.

### Translations

Update both:

- `web_app/app/webapp/semaforky/src/assets/i18n/cs.json`
- `docs/assets/i18n/cs.json`

Replace the Custom Set labels with:

- `Bounced Arrow...`: `Odražený šíp...`
- `Bounced Arrow Time`: `Čas odraženého šípu`
- `Skip`: `Přeskočit`

## Edge cases

- With one line, a bounced arrow is inserted before the next set.
- A canceled set clears any pending bounced-arrow mark.
- Continuous mode continues normally after a bounced arrow completes or is skipped.
- Persist pending/active state so page refresh does not lose it.

## Verification

- Install dependencies with `npm install`.
- Run `npx tsc --noEmit` and `npx ng build`.
- Manually test two-line flow: mark during line one, start the bounced arrow, continue to line two, and test the Skip path.
- Test the last-line boundary and one-line configuration.
- Rebuild deployment artifacts with `make build_webapp && make deploy_testing` if the live demo should be updated.

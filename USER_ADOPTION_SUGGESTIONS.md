# SkySpeak: suggestions for a better app and easier adoption

**Reviewed:** October 27, 2026

**Code baseline:** `ad3f3b9`; `app/build.gradle.kts` declares versionCode 16 / 1.0.15.

**Method:** documentation and source review. No build, device session, usability study, live provider check, or Play Console verification was performed for this document. Findings below distinguish source behavior from proposed improvements; impact and effort are estimates.

SkySpeak already has the ingredients for a useful everyday weather app. The best next investment is to help someone get a forecast, understand what matters, and trust what they see. Prioritize correctness, a first experience that works without location permission, clear freshness indicators, and discovery of existing features before expanding the feature set.

This is a product recommendation document. [IMPROVEMENT_BACKLOG.md](IMPROVEMENT_BACKLOG.md) remains the implementation task list. References to its existing IDs reuse that work; new proposals here should be merged there when selected for implementation.

## 1. What the project already does well

The review covered [README.md](README.md), [CLAUDE.md](CLAUDE.md), the [backlog](IMPROVEMENT_BACKLOG.md), [completed-work record](IMPROVEMENTS.md), the [UX](PREMIUM_UX_ROADMAP.md), [AI](AI_ROADMAP_NEXT_VERSION.md), and [notification](NOTIFICATIONS_ROADMAP.md) roadmaps, the [launch guide](PLAYSTORE_LAUNCH.md), and [privacy page](docs/privacy.html). Code review followed the activity and three screens through their view models, repositories, advice engines, preferences, cache, location provider, notifications, widgets, and supporting rendering/data code. Existing tests cover advice, forecast briefings, and alert tracking.

These capabilities already exist in the reviewed source:

| Existing capability | Why it matters to users | Opportunity |
|---|---|---|
| Weather without an account or weather API key | Little setup is needed | Make that promise visible before asking for location |
| Last-known forecast persisted on the device | Useful content survives offline launches | Explain its age and preserve its location identity |
| Saved cities and city search | Users can browse places and use weather without location permission | Present city search as a first-class starting option |
| Local umbrella, jacket, walking, driving, hiking, and clothing advice | Everyday questions can work without a remote AI request | Expose a few questions directly on the weather screen |
| On-device, plain-language forecast briefing | Makes the forecast easier to interpret | Give its entry point an explicit label |
| Six responsive widget layouts, manual refresh, and cached rendering | Weather can become useful outside the app | Provide an in-app way to discover and add a widget |
| Optional alert and current-conditions notifications | Supports people who rarely open the app | Show whether each feature can actually deliver |
| Themes, units, weather glyph descriptions, and haptic preference | Existing foundations for a comfortable experience | Extend accessibility to controls, charts, and motion |

The source intentionally uses different location rules: the app can show a saved city, while widgets and background notifications follow the device's current location. Preserve that behavior and explain it consistently.

Older roadmap passages describe superseded implementations, including notifications before WorkManager and background location were added. Use source code and the consolidated backlog to establish current behavior. The dated release notes do not establish what is live in Production today.

## 2. Priorities at a glance

**Merged into the backlog 2026-10-27 for versionCode 17:** A1 (already there), A3's core (privacy corrections, Settings privacy link and backup exclusion were already in section 2), A7's search race and empty/error states, A9's chip busy guard (R4 and the error wording were already there), A10's `OptionPill` semantics, and a minimal A2 as a stretch item. The rest is planned for later releases.

**P0:** protect trust before wider promotion. **P1:** improve activation and everyday use. **P2:** expand after validating the basics. Effort is relative: **S** is a focused change; **M** spans several components; **L** changes shared data behavior or needs substantial device validation. These are not delivery commitments.

| ID | Recommendation | Priority | Adoption benefit | Effort | Relationship to existing work |
|---|---|---|---|---|---|
| A1 | Finish forecast and alert correctness fixes | P0 | Prevent misleading information | M | Existing R1–R4, R6–R8a and load-race findings |
| A2 | Make cached data truthful about place, units, and age | P0 | Keep app and widget behavior understandable | M–L | Extends existing offline support; additional findings below |
| A3 | Align privacy controls and disclosures with behavior | P0 | Make data use understandable and controllable | S–M | Existing privacy backlog, plus in-app explanation |
| A4 | Offer city search before requesting location | P1 | Let people get value with fewer permission decisions | M | Refines onboarding #33 and denied-location fix |
| A5 | Make briefings, advice, and widgets discoverable | P1 | Turn existing features into visible benefits | S–M | Extends onboarding #33 |
| A6 | Show notification readiness and open the relevant content | P1 | Reduce silent setup failures and confusing taps | M | Extends notification status item; new navigation work |
| A7 | Improve search, place switching, and state preservation | P1 | Make repeated use feel dependable | M | Existing load race plus additional search issues |
| A8 | Render the core forecast without waiting for optional services | P1 | Reduce waiting on first use | L | Additional recommendation |
| A9 | Make assistant responses clearer and more reliable | P1 | Help users understand and recover from limitations | M | Existing R4/error wording plus additional findings |
| A10 | Complete an accessibility and reduced-motion pass | P1 | Make the app usable by more people | M | Extends existing glyph accessibility |
| A11 | Add lightweight sharing, support, and a feedback loop | P2 | Learn what helps and support organic discovery | S–M | Existing share #30 and review #35; new support path |
| A12 | Localize after separating timestamps from display labels | P2 | Support more languages and time preferences | M–L | Existing localization #28 and field-naming cleanup |

## 3. Recommended changes

### A1. Fix information that can undermine trust

**Observed:** the backlog's correctness issues remain visible in the source. Most concerning, an NWS request failure becomes an empty alert list, which can be interpreted as an alert having ended. Other issues affect metric/imperial wind advice, rain versus snow, remote-city time, the hourly “Now” label, and which place wins concurrent loads.

**Recommend:** complete the existing correctness release before promoting new capabilities. Carry an explicit alert-source state such as available, unavailable, or unsupported; compare alert sets only after a successful fetch. Preserve the last confirmed alert state with its check time during outages. State that its current status could not be confirmed. Keep official alerts visually distinct from locally generated forecast observations.

**Done when:** an alert timeout never produces an “ended” message; equivalent physical conditions produce equivalent advice in both unit systems; snow is labeled correctly; mid-hour and late-night forecasts label time honestly; rapid place changes leave the latest selected place on screen. Add regression coverage at the mapping boundary, where the existing test suite has gaps.

**Source:** [WeatherRepository](app/src/main/java/com/example/weatherly/data/repository/WeatherRepository.kt), [WeatherViewModel](app/src/main/java/com/example/weatherly/ui/WeatherViewModel.kt), [WeatherAlertWorker](app/src/main/java/com/example/weatherly/notifications/WeatherAlertWorker.kt), and backlog section 1. Reuse its detailed fixes rather than creating duplicate tickets.

### A2. Make offline content honest about its place and freshness

**Observed:** `ForecastCache` stores one forecast. The app writes whichever place it is viewing; the widget initially renders that same cache even though its fresh-data path follows the device's location. If the app last showed another city, the widget can temporarily show that city, or retain it when a fresh lookup fails. Widget fresh fetches do not write their result to this persistent cache.

Freshness also has gaps: `WeatherViewModel` saves repository results with the current time even when the repository returns its memory cache. A successful UI state then drops `cachedAt`, and later background-refresh failures remain quiet. The weather screen's cached-age label sits below the metrics and is calculated with `remember(cachedAt)`, so it does not keep aging during an unchanged screen session.

**Recommend:**

- Store forecast identity, unit system, original successful fetch time, and source status with the snapshot. Key snapshots by place/coordinates and units, with a separate reference for the last confirmed device-location snapshot.
- Let widgets reuse the matching device-location snapshot and persist successful widget refreshes. Keep the location name visible wherever space permits. If no matching snapshot exists, offer setup/retry rather than presenting a browsed city's weather as current-location weather.
- Show “Updated 8 minutes ago” near the main location/conditions. On failed refresh, retain the forecast and show “Couldn't refresh · showing saved weather” with Retry.
- Preserve the original fetch time across cache reads. Recalculate age while visible. Clearly label very old forecasts and avoid presenting expired cached alert content as current.

**Done when:** viewing London in-app while physically in New Jersey cannot make the widget silently switch its intended location; a failed lookup remains understandable; restarting the process retains the latest matching snapshot; repeated memory-cache reads do not make old data appear newly fetched. Test unit changes, travel, offline launches, and cached alerts as separate cases.

**Source:** [ForecastCache](app/src/main/java/com/example/weatherly/data/prefs/ForecastCache.kt), `load()` in [WeatherViewModel](app/src/main/java/com/example/weatherly/ui/WeatherViewModel.kt), `getWeather()` in [WeatherRepository](app/src/main/java/com/example/weatherly/data/repository/WeatherRepository.kt), and `provideGlance()` / `fetchFreshWeather()` in [WeatherWidget](app/src/main/java/com/example/weatherly/widget/WeatherWidget.kt). These are source findings, not device reproductions.

### A3. Make privacy understandable inside the app

**Observed:** Settings has a rating link and version number but no privacy-policy link. The privacy backlog already records mismatches about built-in AI access, NOAA, platform geocoding, background requests, and backup. The manifest enables backup, while a user-entered OpenRouter key lives in ordinary app preferences.

**Recommend:** finish those corrections together, then add Privacy, Data sources, and Clear local data under About. Explain what clearing removes before the user acts. Exclude credentials from backups and use Keystore-backed protection for stored user keys. Keep the key field write-only. Before scaling remote AI usage, resolve the existing decision about keeping a limited shared embedded key or moving requests behind a proxy; a proxy would also require updating the client-only privacy description.

Before the first remote AI question, give a short explanation that the question, conversation context, and forecast are sent to OpenRouter and its model provider. Local quick answers should remain available independently. Include source coverage in ordinary help: official advisories are US-only, and the tide feature uses supported US stations. Treat unavailable coverage separately from “no advisories.”

**Done when:** privacy information is reachable from Settings; the disclosure matches the selected answer path; users can clear local data without uninstalling; backup behavior matches the published explanation. Review disclosure text against actual requests rather than treating this document as a legal or store-policy assessment.

**Source:** [privacy page](docs/privacy.html), [manifest](app/src/main/AndroidManifest.xml), [PreferencesStore](app/src/main/java/com/example/weatherly/data/prefs/PreferencesStore.kt), [SettingsScreen](app/src/main/java/com/example/weatherly/ui/SettingsScreen.kt), [ChatRepository](app/src/main/java/com/example/weatherly/data/repository/ChatRepository.kt), and backlog section 2.

### A4. Let people choose a city before asking for location

**Observed:** `WeatherScreen` launches the location permission request on entering composition. A denied user can choose a city, but that path appears after the permission decision. The permission-empty-state copy says the app “needs your location” even though city search works. Returning from Chat or Settings repeats the permission entry path; the denied branch can replace an existing saved-city forecast with a loading screen.

**Recommend:** use one short first-use screen with a useful promise and two clear actions:

> **Weather you can understand at a glance.**
>
> Free, ad-free, and no account required.
>
> **Use my location** · **Search for a city**

Request foreground location only after “Use my location.” Open search directly for the city option. Show the forecast as soon as either path succeeds. Offer optional widget and notification setup later, after the forecast is visible. Remember the completed choice and do not repeat permission requests on screen navigation. Keep an explicit route to change the choice in Locations.

**Done when:** a fresh install can reach a real city forecast without granting location; denial leaves a clear path forward; returning from Chat/Settings retains the forecast and scroll position. Suggested usability target: at least four of five first-time testers complete either path unaided within a minute on a working connection. This is a proposed target, not a measured baseline.

**Source:** permission launcher, `CenterMessage`, and `LocationsSheet` in [WeatherScreen](app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt); `onPermissionDenied()` in [WeatherViewModel](app/src/main/java/com/example/weatherly/ui/WeatherViewModel.kt). Refines backlog onboarding #33: a multi-page tour is unnecessary for the first useful forecast.

### A5. Make existing value easy to find

**Observed:** the briefing opens from a headline pill, local advice lives in Chat, and Settings offers widget appearance without an add-widget action. Users have to discover several capabilities through icons and exploration.

**Recommend:**

- Give the briefing an explicit “Today's outlook” label and a chevron. Reuse `ForecastBriefing` rather than generating a second summary system.
- Add two or three compact quick-question chips below the outlook, such as Umbrella, Jacket, and What to wear. Reuse `WeatherAdvisor`; let the assistant screen hold the full set.
- Add a widget preview and “Add to home screen” action using the system pin flow where supported, with launcher instructions as a fallback. Make dismissal persistent and explain that the widget follows current location.
- Add a short, dismissible hint for tapping a forecast day. Preserve the existing hourly and daily information hierarchy; avoid inserting another large dashboard card.

**Done when:** a new tester can find a plain-language outlook, get one quick answer, open tomorrow's details, and locate widget setup without being coached. Ensure the compact layout remains usable on a small screen and with larger text.

**Source:** `CurrentHeader`, `DailyCard`, and `DetailSheetContent` in [WeatherComponents](app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt), [ForecastBriefing](app/src/main/java/com/example/weatherly/data/advice/ForecastBriefing.kt), [WeatherAdvisor](app/src/main/java/com/example/weatherly/data/advice/WeatherAdvisor.kt), and [SettingsScreen](app/src/main/java/com/example/weatherly/ui/SettingsScreen.kt).

### A6. Make notification setup and notification taps dependable

**Observed:** both notification settings can be On while a missing permission prevents delivery. Background-location and battery explanations already exist, so the gap is a consolidated status for each feature. Notification taps all use the same generic activity intent, which carries neither the alert nor the notification's location. The app can therefore open a previously selected remote city after a notification about the device's location.

**Recommend:** show each feature as Off, Setup incomplete, Ready to check, or Delivery restricted. Under it, show the specific blocker and one relevant action. Check foreground/background location, notification permission, app notification settings, and the applicable notification channel. Show battery restriction separately as a possible delay, not as proof that delivery cannot work.

Record the last successful background check and the last failure reason. Use plain copy such as “Checks periodically; delivery may be delayed.” Label severe-alert notifications as US coverage. Replace recurring missing-location modal reminders with a persistent setup row that users can resolve or turn off. Keep the two features independently optional and retain their current-location behavior.

Pass an alert identifier and location context through notification intents. Handle both cold and already-running activity entry. Open the relevant advisory or current-location forecast; if the advisory is no longer available, explain that instead of opening unrelated content. Preserve the user's saved-city browsing choice for their next normal visit.

**Done when:** every On setting has a truthful readiness state; denied/revoked permissions and blocked channels identify the right next step; a notification about New Jersey cannot silently land on London's forecast. Validate on a Pixel and at least one non-Pixel device, including screen-off use and return from system settings. A test notification may verify display permission/channel behavior, but must not claim to prove background scheduling works.

**Source:** [SettingsScreen](app/src/main/java/com/example/weatherly/ui/SettingsScreen.kt), [WeatherScreen](app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt), [WeatherAlertWorker](app/src/main/java/com/example/weatherly/notifications/WeatherAlertWorker.kt), `contentIntent()` in [WeatherNotifications](app/src/main/java/com/example/weatherly/notifications/WeatherNotifications.kt), and [MainActivity](app/src/main/java/com/example/weatherly/MainActivity.kt).

### A7. Make search and returning to the forecast feel stable

**Observed:** search is debounced in `LocationsSheet`, but each `WeatherViewModel.search()` launches independent work. An older response can overwrite a newer query's results. Shortening the query below two characters does not clear existing results. `searchCity()` returns an empty list on failure, and the sheet has no explicit no-results or search-error state. Saved-city selection and removal already exist.

**Recommend:** cancel or discard superseded searches and forecast loads; clear results when the query becomes too short; represent loading, no matches, and connection failure separately. Keep the existing region/country subtitles in matching results. Preserve weather-screen scroll and relevant input across navigation. When deleting the selected city, offer another saved city or city search if location access is unavailable; add Undo for accidental removal.

**Done when:** typing a second query before the first completes cannot show the first query's results; offline search says it could not connect; clearing the field removes results; switching cities rapidly always lands on the last choice. Returning from Settings or Chat does not reset the user to the top of the forecast.

**Source:** `LocationsSheet` in [WeatherScreen](app/src/main/java/com/example/weatherly/ui/WeatherScreen.kt), `search()`, `load()`, and `removePlace()` in [WeatherViewModel](app/src/main/java/com/example/weatherly/ui/WeatherViewModel.kt), and `searchCity()` in [WeatherRepository](app/src/main/java/com/example/weatherly/data/repository/WeatherRepository.kt).

### A8. Show the core forecast before optional requests finish

**Observed:** forecast, air quality, alerts, and tides start concurrently, but `getWeather()` waits for all of them and then reverse geocoding before returning the screen's data. Catching an optional request's error prevents total failure, but does not remove its waiting time. On first use there may be no cached forecast to show.

**Recommend:** deliver current conditions and hourly/daily forecasts first, then update optional sections independently with bounded waiting. Distinguish loading, unavailable, and unsupported data. Use a location lookup deadline with city search as recovery. Preserve existing cache content during retries. Do not fill missing measurements with a displayed zero merely to draw a chart.

Keep this a focused data-layer change: extract mapping and optional-source status handling into testable units rather than introducing a large framework. Build on A1's alert-state work and A2's timestamp model. Measure before promising a particular startup speed.

**Done when:** deliberately delaying AQI, tides, NWS, or reverse geocoding does not block an already successful core forecast. Later results update only the relevant sections without resetting scroll. Check first install, warm cache, process restart, slow network, and unavailable optional services. Show “Checking advisories” until that source has actually completed.

**Source:** `getWeather()` / `Fetched` / hourly-series mapping in [WeatherRepository](app/src/main/java/com/example/weatherly/data/repository/WeatherRepository.kt), [NetworkModule](app/src/main/java/com/example/weatherly/data/remote/NetworkModule.kt), and [LocationProvider](app/src/main/java/com/example/weatherly/location/LocationProvider.kt).

### A9. Make the assistant's scope and answer path clear

**Observed:** the assistant combines deterministic local advice and remote AI. Local answers deliberately simulate streaming, which adds waiting to an already computed answer. The showcased “Best day this week for a long run?” hits a current-conditions local intent, as recorded in R4. Input sending is guarded, but suggestion chips can call `sendLocal()` during another response; that path has no equivalent busy guard and shares the streaming state. Conversations also survive navigation while their weather context can change.

**Recommend:**

- Fix future-time routing before promoting planning prompts. Use examples that work end to end with the available forecast horizon.
- Render local answers immediately and label them “On-device answer.” Label remote responses “AI answer,” with the forecast location and freshness available nearby. Describe offline advice as using the saved forecast when applicable.
- Put key and model controls under “Advanced AI settings.” Represent built-in access, a user's own key, and no available key accurately; show cap and service-unavailable messages that fit each case.
- Offer Retry after a failed remote request and Stop while it is running. Keep one active response: disable quick chips or explicitly cancel the prior response before starting another. Fix error directions that refer to a nonexistent key field in Chat.
- When the location or forecast context changes, mark that change and start a fresh conversation or explicitly associate messages with their earlier context. Do not silently carry another city's conversation into the new forecast.

**Done when:** quick advice works from a cached forecast without a key or network and appears without artificial typing delays; weekly questions reach the planning path; rapid chip taps cannot mix responses; a shared-service failure never tells a user to fund an account they do not have. A3's explanation should precede the first remote send.

**Source:** [ChatScreen](app/src/main/java/com/example/weatherly/ui/ChatScreen.kt), [ChatViewModel](app/src/main/java/com/example/weatherly/ui/ChatViewModel.kt), [ChatRepository](app/src/main/java/com/example/weatherly/data/repository/ChatRepository.kt), [WeatherAdvisor](app/src/main/java/com/example/weatherly/data/advice/WeatherAdvisor.kt), and [SettingsScreen](app/src/main/java/com/example/weatherly/ui/SettingsScreen.kt).

### A10. Support larger text, assistive technology, and less motion

**Observed:** weather glyphs already provide descriptions. Custom settings pills communicate selection visually but use plain `clickable` without selected/toggle semantics. Several charts draw data on Canvas without a spoken trend summary. The assistant entry is a custom 40 dp control, widget refresh is a small text action, and the animated weather background has a continuous frame loop with no reduced-motion preference.

**Recommend:** use standard selectable/toggle components or equivalent role and state semantics. Add concise chart summaries, for example “Wind rises from 10 to 25 km/h by 4 PM,” and a readable detail alternative. Make the briefing's purpose clear to screen readers. Use 48 dp touch targets as a design target where the host permits, and validate compact widget controls separately.

Add an app motion preference and respect system animation preferences where applicable. Static mode should retain condition colors and text while stopping particles, flashes, pulsing dots, and elaborate transitions. Pause unnecessary animation when the app is not visible. Keep the existing haptic preference independent.

**Done when:** a TalkBack user can choose a city, understand selected units, read the forecast, and open/close details. At 200% font size on a compact phone, key actions and values remain reachable. Static mode preserves all information and actually stops the custom animation work. These are proposed acceptance checks, not completed accessibility certification or measured performance claims.

**Source:** `OptionPill` in [SettingsScreen](app/src/main/java/com/example/weatherly/ui/SettingsScreen.kt), [WeatherGlyph](app/src/main/java/com/example/weatherly/ui/components/WeatherGlyph.kt), chart composables in [WeatherComponents](app/src/main/java/com/example/weatherly/ui/components/WeatherComponents.kt), [WeatherBackground](app/src/main/java/com/example/weatherly/ui/components/WeatherBackground.kt), and [WeatherWidget](app/src/main/java/com/example/weatherly/widget/WeatherWidget.kt).

### A11. Make feedback and sharing easy without adding tracking

**Observed:** the app intentionally has no analytics SDK. A manual Play rating link already exists, but Settings has no report-a-problem/help action. Sharing and screenshot captions are already backlog items.

**Recommend:** add a support action with an editable report containing app version, Android version, and the user's description. Include coordinates, forecast details, or logs only if the user explicitly chooses them and can preview the content. Add a compact help page covering city mode, offline age, widget setup, alert coverage, and periodic notifications.

Implement the existing share proposal first as text: city, forecast period, temperature, short outlook, source/fetch time, and app link. Let the user choose whether to include their location. Update store screenshot captions around tasks: “Understand today's outlook,” “Ask a quick weather question,” and “Weather on your home screen.” Check the actual released build before describing its capabilities. Revisit whether “Premium Weather Chat” makes the free, account-free utility clear to new users.

Start with recruited user sessions and voluntary feedback. Optional internal-build counters can help testers inspect their own usage, but local counters do not provide aggregate retention unless users deliberately share them. Keep daily digest and automatic review prompts behind evidence that people want them; a digest needs its own opt-in and timing controls.

**Done when:** a tester can report a problem with enough non-sensitive context to reproduce it; shared text identifies its forecast time; store promises match the released app. Do not infer that chat is unpopular, or widgets improve retention, from the current absence of telemetry.

**Source:** About in [SettingsScreen](app/src/main/java/com/example/weatherly/ui/SettingsScreen.kt), [launch guide](PLAYSTORE_LAUNCH.md), [AI discussion](AI_ROADMAP_NEXT_VERSION.md), and backlog product items #30, #34, and #35.

### A12. Prepare localization without breaking forecast logic

**Observed:** `strings.xml` contains only the app name. User-facing strings live throughout Kotlin. Some forecast logic parses display labels such as “12 AM”; the documentation explicitly preserves US-format labels to keep those calculations working. Unit-suffixed model names such as `currentTempC` can hold the selected user's units, which has contributed to existing bugs.

**Recommend:** put actual timestamps, time zones, and explicit unit identity in the domain model before localizing display labels. Compute day boundaries from timestamps. Then extract UI, error, notification, and advice strings into resources with plurals and formatting placeholders. Support the device's 12/24-hour choice and test longer strings and right-to-left layouts. Select the first additional language from actual tester demand.

**Done when:** changing device language or time format cannot change which forecast hours count as today; daily and main hourly views agree; cached-model migration is tested; a non-English user can complete onboarding and read advice. Treat additional advice-language support as real scope: translating labels alone will not translate English keyword routing.

**Source:** [strings.xml](app/src/main/res/values/strings.xml), [WeatherData](app/src/main/java/com/example/weatherly/data/model/WeatherData.kt), time selection/formatting in [WeatherRepository](app/src/main/java/com/example/weatherly/data/repository/WeatherRepository.kt), [WeatherAdvisor](app/src/main/java/com/example/weatherly/data/advice/WeatherAdvisor.kt), and the localization constraints in [CLAUDE.md](CLAUDE.md).

## 4. Suggested delivery sequence

| Increment | Scope | Exit condition |
|---|---|---|
| Trust | A1 correctness, A2 minimum cache identity/freshness fixes, A3 disclosure/credential work | Failures never masquerade as fresh data or resolved alerts; cached place and units are truthful |
| First useful session | A4 city/location choice, A5 compact discovery, A7 search/state fixes | New users can load weather and find one useful action without coaching |
| Dependable daily use | A6 readiness and notification destinations, A9 assistant fixes, A10 accessibility | Setup failures have clear recovery; returning users and assistive-technology users can complete core tasks |
| Measured expansion | A8 staged loading, remaining A2 cache work, A11 feedback/sharing, A12 localization | Changes address observed waiting, comprehension, support, or language needs |

Begin the accessibility pass during onboarding work, even if its broader checks finish later. A8 depends on explicit source states and freshness metadata; bring it forward if startup measurements show optional requests are a dominant blocker. Split increments into reviewable changes rather than one broad UI rewrite.

For future code changes, add focused repository/mapping and state-transition tests alongside the existing advice tests. Use device checks for permission flows, widget rendering, screen navigation, and notification routing. A checked-in Gradle wrapper and basic CI would make that verification easier to reproduce; the current documented CLI workflow depends on locally cached tools. This documentation-only review did not rerun the historical test results.

## 5. How to tell whether adoption improved

Use a first usability round with five to eight people: permission-denying city users, people unfamiliar with AI tools, a large-text or TalkBack user, and at least one non-Pixel notification/widget tester. Ask each to get local weather, inspect tomorrow, ask an everyday question, add a widget, and explain whether notifications are ready. Avoid explaining the interface before they attempt the tasks.

| Question | Evidence to collect | Proposed initial success criterion |
|---|---|---|
| Can users get started? | Time and assistance needed to reach the first real forecast | At least 4/5 complete within one minute on a working connection |
| Do users understand saved data? | Ask what place/time the offline forecast describes | At least 4/5 identify both correctly |
| Can users find existing value? | Observe outlook, quick advice, tomorrow detail, and widget setup tasks | At least 4/5 find each without hints |
| Is notification setup honest? | Exercise denied permission, blocked channel, and successful check states | Every tested blocker has a visible, correct explanation |
| Is the app comfortable to use? | Large text, TalkBack, compact screen, reduced-motion checks | Core tasks complete without hidden actions or unreadable values |
| Does usefulness persist? | Voluntary follow-up after a week; tester-shared local counters if used | Establish a baseline and learn which tasks recur before setting a retention target |

These are research targets, not product metrics already collected. Play Console crash/ANR information and voluntary reports can supplement the sessions; they cannot establish feature adoption on their own. If aggregate analytics is considered later, make that an explicit product/privacy decision.

## 6. Ideas to defer

Keep voice, gyroscope effects, blur/shader experiments, additional forecast providers, minute-level precipitation promises, subscriptions, accounts, and cloud sync behind evidence of a specific unmet need. They add decisions, operational work, or compatibility paths before the existing value is easy to use. Recheck provider capabilities and terms if a data-source or commercial-model change is selected; older roadmap claims are not current verification.

Respect the recorded decisions around removed radar, lock-screen widgets, lifestyle profiling, opaque cards, and current-location background surfaces. Revisit them only with new evidence and an explicit scope decision. The next adoption gains are most likely to come from a forecast users can reach quickly, interpret easily, and trust when data or permissions are missing.

# Trip Planner App

Trip Mate is an Android app that lets users plan trips, get AI-generated itineraries, receive reminders, and save trip memories. It is built with Java, Firebase (Auth + Realtime Database), Google Places, and a Gemini-based AI service.

## 5. Development

### Project Structure
- `app/src/main/java/com/vaatu/tripmate/` – core source
  - `ui/` – screens (auth, splash, upcoming trips, AI planner, memories)
  - `data/remote/network/` – Firebase DB wrapper, Gemini AI client, AI plan cache
  - `service/` – foreground/overlay services for alerts
  - `utils/` – models (trips, users, photos), helpers, AI DTOs
- `app/src/main/res/layout/` – XML screens (activities, fragments, list rows)
- `app/src/main/AndroidManifest.xml` – permissions, activities, services, receivers
- `app/src/test` & `app/src/androidTest` – sample unit and instrumented tests

### XML Layout Screens
- Activities: `activity_splash`, `activity_user_cycle`, `activity_upcoming_trips`, `activity_add_btn`, `activity_ai_trip_plan`, `activity_memories`, `activity_trip_memories`, `activity_my_dialog`
- Fragments: `fragment_home`, `history_fragment`, `fragment_gallery`, `fragment_slideshow`
- Lists/cards: `custom_card_row`, `history_card_row`, `item_day_plan`, `item_packing_list`, `item_memories_trip`, `item_photo`
- Auth: `login`, `up_sign`

### Java Code Components
- Activities/Fragments: splash, `UserCycleActivity`, `Login`/`SignUp`, `UpcomingTripsActivity` (drawer + nav), `AddBtnActivity` (trip creator/editor + alarms), `HomeFragment` (upcoming list), `HistoryFragment`, AI planner (`AiTripPlanActivity`), memories (`MemoriesActivity`, `TripMemoriesActivity`)
- Adapters: `HomeAdaptor`, `HistoryAdaptor`, `DayPlanAdapter`, `PackingListAdapter`, `MemoriesAdapter`, `PhotosAdapter`
- Models: `TripModel`, `UserModel`, `TripPhoto`, AI DTOs (`AiTripPlan`, `PackingItem`, `DayPlan`, `PlaceToVisit`, `EstimatedCosts`)
- Services: `DialognotificationService`, `FloatingWindowService`; receiver `AlarmEventReciever`
- Network/DB: `FirebaseDB` (trip CRUD/history), `AiTripPlanFirebaseService` (cache AI plans per user), `GeminiTravelService` (Gemini API call + parsing)

### Database / Storage
- Firebase Realtime Database (`https://trip-mate-7fac8-default-rtdb.firebaseio.com/`) stores users, upcoming trips, history, cached AI plans (scoped by user UID).
- Firebase Auth handles email/password login.
- Local storage: `LocalPhotoStore` persists trip photos metadata in `SharedPreferences`, files saved under app internal storage.
- External APIs: Google Places Autocomplete for origin/destination; Gemini model for AI plans (API key currently in code).

## 6. Modules Description
- **Module 1 – User & Session**: Splash → auth flow (`Login`, `SignUp`), routes authenticated users to `UpcomingTripsActivity`; stores user profile in Firebase.
- **Module 2 – Trip Management & Reminders**: Create/edit trips with Places autocomplete, notes, date/time; saves to Firebase upcoming list, moves to history on completion/cancel; schedules alarms via `AlarmEventReciever`; navigation drawer for home/history/sync.
- **Module 3 – AI Planner & Memories**: `AiTripPlanActivity` fetches or caches itineraries/packing lists via Gemini + Firebase; memories flow lists past trips and lets users attach/view/delete locally stored photos.

## 7. Implementation

### Important Code Snippets
```
51:115:app/src/main/java/com/vaatu/tripmate/data/remote/network/GeminiTravelService.java
public void generatePlan(TripModel trip, GeminiCallback callback) {
    if (trip == null) { callback.onError("Missing trip data."); return; }
    // build JSON prompt, call Gemini, parse AiTripPlan
    client.newCall(request).enqueue(new Callback() {
        public void onFailure(Call call, IOException e) { callback.onError("Network error: " + e.getMessage()); }
        public void onResponse(Call call, Response response) throws IOException {
            if (!response.isSuccessful()) { callback.onError("Gemini API error: " + response.code()); return; }
            AiTripPlan plan = parsePlan(response.body().string());
            if (plan == null || plan.isEmpty()) callback.onError("Unable to parse AI response.");
            else callback.onSuccess(plan);
        }
    });
}
```
```
188:257:app/src/main/java/com/vaatu/tripmate/data/remote/network/AiTripPlanFirebaseService.java
public void loadAiTripPlan(String tripId, AiTripPlanCallback callback) {
    DatabaseReference planRef = getAiTripPlanRef(tripId);
    planRef.addListenerForSingleValueEvent(new ValueEventListener() {
        public void onDataChange(DataSnapshot snapshot) {
            AiTripPlan plan = snapshot.getValue(AiTripPlan.class);
            if (plan != null && !plan.isEmpty()) callback.onSuccess(plan);
            else callback.onPlanNotFound();
        }
        public void onCancelled(DatabaseError error) { callback.onError(error.getMessage()); }
    });
}
public void saveAiTripPlan(String tripId, AiTripPlan plan) {
    plan.setCreatedAt(System.currentTimeMillis());
    getAiTripPlanRef(tripId).setValue(plan);
}
```
```
209:473:app/src/main/java/com/vaatu/tripmate/ui/home/addButtonActivity/AddBtnActivity.java
@OnClick({R.id.add_trip_btn, ...})
public void onViewClicked(View view) {
    if (view.getId() == R.id.add_trip_btn) {
        TripModel newTrip = new TripModel(selectedStartPlace, selectedEndPlace,
            dateTextField.getText().toString(), timeTextField.getText().toString(),
            tripNameTextField.getEditText().getText().toString(), null, notesList, mCalendar.getTime().toString());
        if (isEdit) tripsRef.child(tripKey).setValue(newTrip);
        else { startAlarm(newTrip); setResult(Activity.RESULT_OK, resultIntent); }
    }
}
private void startAlarm(TripModel tripModel) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(sdf.parse(tripModel.dateTime));
    PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);
    alarmManager.set(AlarmManager.RTC, cal.getTimeInMillis(), pi);
}
```

### App Permissions
From `AndroidManifest.xml`: `INTERNET`, `ACCESS_NETWORK_STATE`, `READ/WRITE_EXTERNAL_STORAGE`, `READ_PHONE_STATE`, `SYSTEM_ALERT_WINDOW`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`. Overlay permission is requested at runtime for floating windows; external storage is requested when adding photos.

### Data Handling & Processing
- Trips: created/edited in `AddBtnActivity`, posted to Firebase (`trip-mate/{uid}/upcomingtrips`); start/cancel moves entries between upcoming and history via `FirebaseDB`.
- AI plans: `AiTripPlanActivity` generates prompt → Gemini → parsed JSON → displayed and cached per `tripId` (MD5 hash of trip fields) in Firebase; packing item checkboxes persist to cache.
- Memories: past trips fetched from Firebase history; photos stored locally per `tripId` (metadata in SharedPreferences, files in internal storage) with Glide thumbnails.
- Notifications: alarms scheduled with `AlarmManager` and `AlarmEventReciever`; navigation intent to Google Maps on “Start now”.

## 8. Testing
- Automated: only template tests (`ExampleUnitTest` addition check; `ExampleInstrumentedTest` app context). No feature-level unit/UI tests are present.
- Manual suggestions: auth flows (sign-up/login/reset), add/edit trip with alarms, AI plan fetch with/without cache, history move/cancel, photo add/delete, overlay permission handling.

## 9. Conclusion
- Summary: Firebase-backed trip planner with AI-generated itineraries, reminders, and local photo memories. Core flows use RecyclerViews + Firebase listeners for live updates; AI plans cached per trip to reduce API calls.
- Limitations: hard-coded API keys (Gemini, Places, HERE) in repo; minimal input validation/error states; alarms use deprecated exact set without channel handling; storage uses legacy external storage permission; limited automated tests.
- Future Scope: secure key management (remote config/secrets), add offline sync and pagination, migrate photo storage to cloud bucket, add Room cache for trips, expand test suite (unit/UI), support dark mode and accessibility, add analytics and crash reporting.


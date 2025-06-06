package dao.impl;

import com.google.api.client.auth.oauth2.Credential;
// import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
// import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Events;

import dao.ScheduleDAO;
import model.Event;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleCalendarDAO implements ScheduleDAO {

    private static final String APPLICATION_NAME = "Telegram Scheduler Bot";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String OOB_REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

    private final NetHttpTransport httpTransport;
    private final GoogleClientSecrets clientSecrets;

    public GoogleCalendarDAO() throws GeneralSecurityException, IOException {
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        InputStream in = GoogleCalendarDAO.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        this.clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
    }

    private DataStoreFactory createDataStoreFactory(Long ownerChatId) throws IOException {
        return new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH, ownerChatId.toString()));
    }
    
    private GoogleAuthorizationCodeFlow createGoogleAuthorizationCodeFlow(Long ownerChatId) throws IOException {
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(createDataStoreFactory(ownerChatId))
                .setAccessType("offline")
                .build();
    }

    public Credential loadCredential(Long ownerChatId) throws IOException {
        GoogleAuthorizationCodeFlow flow = createGoogleAuthorizationCodeFlow(ownerChatId);
        return flow.loadCredential(ownerChatId.toString());
    }

    public String getAuthorizationUrl(Long ownerChatId, String state) throws IOException {
        GoogleAuthorizationCodeFlow flow = createGoogleAuthorizationCodeFlow(ownerChatId);
        return flow.newAuthorizationUrl()
                .setRedirectUri(OOB_REDIRECT_URI)
                .setState(state)
                .build();
    }

    public Credential exchangeCodeForTokens(Long ownerChatId, String authorizationCode) throws IOException {
        GoogleAuthorizationCodeFlow flow = createGoogleAuthorizationCodeFlow(ownerChatId);
        com.google.api.client.auth.oauth2.TokenResponse response = flow.newTokenRequest(authorizationCode)
                .setRedirectUri(OOB_REDIRECT_URI)
                .execute();
        return flow.createAndStoreCredential(response, ownerChatId.toString());
    }
    
    public void deleteTokens(Long ownerChatId) throws IOException {
        // DataStoreFactory dataStoreFactory = createDataStoreFactory(ownerChatId);
        java.io.File userTokenDir = new java.io.File(TOKENS_DIRECTORY_PATH, ownerChatId.toString());
        if (userTokenDir.exists()) {
            java.io.File storedCredentialFile = new java.io.File(userTokenDir, "StoredCredential");
            if (storedCredentialFile.exists()) {
                if (storedCredentialFile.delete()) {
                    System.out.println("[GoogleCalendarDAO] Deleted StoredCredential for user: " + ownerChatId);
                } else {
                    System.err.println("[GoogleCalendarDAO] Failed to delete StoredCredential for user: " + ownerChatId);
                }
            }
            if (userTokenDir.isDirectory() && userTokenDir.list().length == 0) {
                if (userTokenDir.delete()) {
                     System.out.println("[GoogleCalendarDAO] Deleted token directory for user: " + ownerChatId);
                }
            }
        }
    }

    @Override
    public List<Event> getEvents(LocalDate from, LocalDate to, Long ownerChatId) throws GeneralSecurityException, IOException {
        Credential credential = loadCredential(ownerChatId);
        if (credential == null || credential.getAccessToken() == null) {
            throw new UserNotAuthenticatedException("User " + ownerChatId + " is not authenticated with Google Calendar. Please use /connect_google_calendar.");
        }

        Calendar service = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        List<Event> resultEvents = new ArrayList<>();
        DateTime dateFrom = new DateTime(from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        DateTime dateTo = new DateTime(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

        String calendarId = "primary";

        Events googleEventsResponse = service.events().list(calendarId)
                .setTimeMin(dateFrom)
                .setTimeMax(dateTo)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        List<com.google.api.services.calendar.model.Event> items = googleEventsResponse.getItems();
        if (items != null) {
            for (com.google.api.services.calendar.model.Event googleEvent : items) {
                resultEvents.add(convertToModelEvent(googleEvent));
            }
        }
        return resultEvents;
    }

    public List<Event> getEvents(LocalDate from, LocalDate to) throws Exception {
        System.err.println("[GoogleCalendarDAO] WARNING: getEvents(from, to) called without ownerChatId. This may not work correctly in multi-user mode or use default/first user tokens.");
        System.err.println("[GoogleCalendarDAO] CRITICAL: getEvents(from, to) without ownerChatId is not suitable for multi-user. Returning empty list or throwing error.");
        throw new UnsupportedOperationException("getEvents(LocalDate, LocalDate) is deprecated. Use getEvents(LocalDate, LocalDate, Long ownerChatId).");
    }

    private Event convertToModelEvent(com.google.api.services.calendar.model.Event googleEvent) {
        String googleCalendarEventId = googleEvent.getId();
        String title = googleEvent.getSummary();
        String description = googleEvent.getDescription();
        String location = googleEvent.getLocation();

        LocalDateTime startTime = convertGoogleDateTimeToLocalDateTime(googleEvent.getStart());
        LocalDateTime endTime = convertGoogleDateTimeToLocalDateTime(googleEvent.getEnd());
        
        LocalDateTime reminderTime = null;
        boolean remindersEnabledInGoogle = true; 
        if (googleEvent.getReminders() != null) {
            if (Boolean.FALSE.equals(googleEvent.getReminders().getUseDefault())) {
                remindersEnabledInGoogle = googleEvent.getReminders().getOverrides() != null && !googleEvent.getReminders().getOverrides().isEmpty();
            }
        }
        Event modelEvent = new Event(); 
        modelEvent.setGoogleId(googleCalendarEventId);
        modelEvent.setTitle(title);
        modelEvent.setDescription(description);
        modelEvent.setStartTime(startTime);
        modelEvent.setEndTime(endTime);
        modelEvent.setLocation(location);
        modelEvent.setReminderTime(reminderTime);
        modelEvent.setRemindersEnabled(remindersEnabledInGoogle);
        return modelEvent;
    }

    private LocalDateTime convertGoogleDateTimeToLocalDateTime(com.google.api.services.calendar.model.EventDateTime eventDateTime) {
        if (eventDateTime == null) {
            return null;
        }
        DateTime dateTime = eventDateTime.getDateTime();
        if (dateTime != null) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime.getValue()), ZoneId.systemDefault());
        } else if (eventDateTime.getDate() != null) {
            String dateString = eventDateTime.getDate().toStringRfc3339().substring(0, 10);
            LocalDate localDate = LocalDate.parse(dateString);
            return localDate.atStartOfDay();
        }
        return null;
    }
}

// Определение класса UserNotAuthenticatedException было перемещено в отдельный файл: UserNotAuthenticatedException.java 
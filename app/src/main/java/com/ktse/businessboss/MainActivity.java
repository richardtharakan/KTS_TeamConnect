package com.ktse.businessboss;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String BOSS_PIN = "1234";
    private static final String MANAGER_PIN = "1111";
    private static final String PREFS_NAME = "kts_connect_team_prefs";
    private static final String KEY_ROLE = "saved_role";

    private LinearLayout loginBox;
    private LinearLayout appContentBox;
    private EditText rolePinInput;
    private Button enterBossButton;
    private Button enterManagerButton;
    private Button logoutButton;
    private TextView loginStatusText;

    private TextView companyTitleText;
    private TextView modeBadgeText;
    private TextView dashboardSummaryText;
    private TextView managerCardText;
    private TextView attendanceCardText;
    private TextView attendanceHistoryText;
    private TextView currentTaskText;
    private TextView taskSummaryText;
    private TextView statusText;

    private Button checkInButton;
    private Button checkOutButton;
    private Button reassignTaskButton;
    private Button assignTaskButton;
    private Button filterAllButton;
    private Button filterPendingButton;
    private Button filterCompletedButton;

    private EditText taskTitleInput;
    private EditText taskDescriptionInput;
    private Spinner taskPrioritySpinner;
    private EditText taskDueDateInput;

    private LinearLayout assignTaskBox;
    private LinearLayout managerActionBox;
    private LinearLayout taskListContainer;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private DocumentReference companyRef;
    private DocumentReference managerRef;
    private DocumentReference attendanceRef;
    private DocumentReference currentTaskRef;

    private ListenerRegistration companyListener;
    private ListenerRegistration managerListener;
    private ListenerRegistration attendanceListener;
    private ListenerRegistration attendanceHistoryListener;
    private ListenerRegistration tasksListener;

    private QuerySnapshot latestTasksSnapshot;

    private final Map<String, Uri> selectedAttachmentUris = new HashMap<>();
    private final Map<String, String> selectedAttachmentNames = new HashMap<>();
    private String pendingAttachmentTaskId = "";

    private String currentTaskId = "task_1";
    private String managerId = "manager_1";
    private boolean isBossView = true;
    private String taskFilter = "all";

    private String dashboardBusinessName = "Global Plaza";
    private String dashboardManagerName = "Manager 1";
    private boolean dashboardManagerCheckedIn = false;
    private String dashboardTodayAttendanceStatus = "-";
    private String dashboardCheckInTime = "-";
    private String dashboardCheckOutTime = "-";
    private int dashboardPendingTasks = 0;
    private int dashboardCompletedTasks = 0;
    private int dashboardTotalTasks = 0;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupFirebase();
        setupPrioritySpinner();
        setupFilePicker();
        setupClicks();

        startRealtimeListeners();
        restoreSavedRoleOrShowLogin();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRealtimeListeners();
    }

    private void bindViews() {
        loginBox = findViewById(R.id.loginBox);
        appContentBox = findViewById(R.id.appContentBox);
        rolePinInput = findViewById(R.id.rolePinInput);
        enterBossButton = findViewById(R.id.enterBossButton);
        enterManagerButton = findViewById(R.id.enterManagerButton);
        logoutButton = findViewById(R.id.logoutButton);
        loginStatusText = findViewById(R.id.loginStatusText);

        companyTitleText = findViewById(R.id.companyTitleText);
        modeBadgeText = findViewById(R.id.modeBadgeText);
        dashboardSummaryText = findViewById(R.id.dashboardSummaryText);
        managerCardText = findViewById(R.id.managerCardText);
        attendanceCardText = findViewById(R.id.attendanceCardText);
        attendanceHistoryText = findViewById(R.id.attendanceHistoryText);
        currentTaskText = findViewById(R.id.currentTaskText);
        taskSummaryText = findViewById(R.id.taskSummaryText);
        statusText = findViewById(R.id.statusText);

        checkInButton = findViewById(R.id.checkInButton);
        checkOutButton = findViewById(R.id.checkOutButton);
        reassignTaskButton = findViewById(R.id.reassignTaskButton);
        assignTaskButton = findViewById(R.id.assignTaskButton);
        filterAllButton = findViewById(R.id.filterAllButton);
        filterPendingButton = findViewById(R.id.filterPendingButton);
        filterCompletedButton = findViewById(R.id.filterCompletedButton);

        taskTitleInput = findViewById(R.id.taskTitleInput);
        taskDescriptionInput = findViewById(R.id.taskDescriptionInput);
        taskPrioritySpinner = findViewById(R.id.taskPrioritySpinner);
        taskDueDateInput = findViewById(R.id.taskDueDateInput);

        assignTaskBox = findViewById(R.id.assignTaskBox);
        managerActionBox = findViewById(R.id.managerActionBox);
        taskListContainer = findViewById(R.id.taskListContainer);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        companyRef = db.collection("companies").document("global_plaza");
        managerRef = companyRef.collection("users").document(managerId);
        attendanceRef = companyRef.collection("attendance").document(getTodayAttendanceId());
    }

    private void setupPrioritySpinner() {
        String[] priorities = {"Normal", "High", "Low", "Urgent"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                priorities
        );

        taskPrioritySpinner.setAdapter(adapter);
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        statusText.setText("No file selected.");
                        return;
                    }

                    Uri uri = result.getData().getData();

                    if (uri == null || pendingAttachmentTaskId.isEmpty()) {
                        statusText.setText("File selection failed.");
                        return;
                    }

                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception ignored) {
                    }

                    String fileName = getFileNameFromUri(uri);

                    selectedAttachmentUris.put(pendingAttachmentTaskId, uri);
                    selectedAttachmentNames.put(pendingAttachmentTaskId, fileName);

                    statusText.setText("Selected file for task: " + fileName);

                    if (latestTasksSnapshot != null) {
                        renderTaskList(latestTasksSnapshot);
                    }
                }
        );
    }

    private void setupClicks() {
        enterBossButton.setOnClickListener(v -> attemptBossLogin());
        enterManagerButton.setOnClickListener(v -> attemptManagerLogin());
        logoutButton.setOnClickListener(v -> logoutAndShowLogin());

        checkInButton.setOnClickListener(v -> managerCheckIn());
        checkOutButton.setOnClickListener(v -> managerCheckOut());
        reassignTaskButton.setOnClickListener(v -> bossSetCurrentTaskPending());
        assignTaskButton.setOnClickListener(v -> bossAssignNewTask());

        filterAllButton.setOnClickListener(v -> applyTaskFilter("all"));
        filterPendingButton.setOnClickListener(v -> applyTaskFilter("pending"));
        filterCompletedButton.setOnClickListener(v -> applyTaskFilter("completed"));

        taskDueDateInput.setOnClickListener(v -> openDueDatePicker());
    }

    private void restoreSavedRoleOrShowLogin() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedRole = prefs.getString(KEY_ROLE, "");

        if ("boss".equals(savedRole)) {
            loginBox.setVisibility(View.GONE);
            appContentBox.setVisibility(View.VISIBLE);
            showBossView();
        } else if ("manager".equals(savedRole)) {
            loginBox.setVisibility(View.GONE);
            appContentBox.setVisibility(View.VISIBLE);
            showManagerView();
        } else {
            showLoginScreen();
        }
    }

    private void showLoginScreen() {
        loginBox.setVisibility(View.VISIBLE);
        appContentBox.setVisibility(View.GONE);
        rolePinInput.setText("");
        loginStatusText.setText("Test PINs: Boss 1234 / Manager 1111");
    }

    private void logoutAndShowLogin() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .remove(KEY_ROLE)
                .apply();

        showLoginScreen();
    }

    private void attemptBossLogin() {
        String pin = rolePinInput.getText().toString().trim();

        if (BOSS_PIN.equals(pin)) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_ROLE, "boss")
                    .apply();

            loginBox.setVisibility(View.GONE);
            appContentBox.setVisibility(View.VISIBLE);
            showBossView();
        } else {
            loginStatusText.setText("Wrong Boss PIN.");
        }
    }

    private void attemptManagerLogin() {
        String pin = rolePinInput.getText().toString().trim();

        if (MANAGER_PIN.equals(pin)) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_ROLE, "manager")
                    .apply();

            loginBox.setVisibility(View.GONE);
            appContentBox.setVisibility(View.VISIBLE);
            showManagerView();
        } else {
            loginStatusText.setText("Wrong Manager PIN.");
        }
    }

    private void showBossView() {
        isBossView = true;

        modeBadgeText.setText("Current Mode: Boss");
        assignTaskBox.setVisibility(View.VISIBLE);
        managerActionBox.setVisibility(View.GONE);
        reassignTaskButton.setVisibility(View.VISIBLE);
        attendanceHistoryText.setVisibility(View.VISIBLE);

        statusText.setText("Boss View - Live sync active ✅");
        renderDashboardSummary();

        if (latestTasksSnapshot != null) {
            renderTaskList(latestTasksSnapshot);
        }
    }

    private void showManagerView() {
        isBossView = false;

        modeBadgeText.setText("Current Mode: Manager");
        assignTaskBox.setVisibility(View.GONE);
        managerActionBox.setVisibility(View.VISIBLE);
        reassignTaskButton.setVisibility(View.GONE);
        attendanceHistoryText.setVisibility(View.GONE);

        statusText.setText("Manager View - Live sync active ✅");
        renderDashboardSummary();

        if (latestTasksSnapshot != null) {
            renderTaskList(latestTasksSnapshot);
        }
    }

    private void openDueDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String dateText = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );

                    taskDueDateInput.setText(dateText);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void applyTaskFilter(String filter) {
        taskFilter = filter;

        if (latestTasksSnapshot != null) {
            renderTaskList(latestTasksSnapshot);
        }

        statusText.setText("Task filter applied: " + getTaskFilterLabel());
    }

    private void startRealtimeListeners() {
        stopRealtimeListeners();

        companyListener = companyRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                statusText.setText("Company live sync failed: " + error.getMessage());
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                String businessName = documentSnapshot.getString("businessName");

                if (businessName != null && !businessName.isEmpty()) {
                    dashboardBusinessName = businessName;
                    companyTitleText.setText(businessName);
                }

                String savedTaskId = documentSnapshot.getString("currentTaskId");

                if (savedTaskId != null && !savedTaskId.isEmpty()) {
                    currentTaskId = savedTaskId;
                } else {
                    currentTaskId = "task_1";
                }

                currentTaskRef = companyRef.collection("tasks").document(currentTaskId);

                if (latestTasksSnapshot != null) {
                    renderCurrentTaskFromSnapshot(latestTasksSnapshot);
                } else {
                    loadCurrentTaskOnce();
                }

                renderDashboardSummary();

                if (appContentBox.getVisibility() == View.VISIBLE) {
                    statusText.setText("Live sync active ✅");
                }
            }
        });

        managerListener = managerRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                managerCardText.setText("Manager live sync failed:\n" + error.getMessage());
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String email = documentSnapshot.getString("email");
                Boolean checkedIn = documentSnapshot.getBoolean("checkedIn");

                dashboardManagerName = safeText(name);
                dashboardManagerCheckedIn = Boolean.TRUE.equals(checkedIn);

                String attendanceStatus = dashboardManagerCheckedIn
                        ? "Checked In ✅"
                        : "Not Checked In ❌";

                String text =
                        "Manager Status\n\n" +
                                "Name: " + safeText(name) + "\n" +
                                "Email: " + safeText(email) + "\n" +
                                "Attendance: " + attendanceStatus;

                managerCardText.setText(text);
                renderDashboardSummary();
            } else {
                managerCardText.setText("Manager document not found.");
            }
        });

        attendanceListener = attendanceRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                attendanceCardText.setText("Attendance live sync failed:\n" + error.getMessage());
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                String date = documentSnapshot.getString("date");
                String status = documentSnapshot.getString("status");
                Boolean checkedIn = documentSnapshot.getBoolean("checkedIn");

                Timestamp checkInAt = documentSnapshot.getTimestamp("checkInAt");
                Timestamp checkOutAt = documentSnapshot.getTimestamp("checkOutAt");

                String checkInText = formatTimestamp(checkInAt);
                String checkOutText = formatTimestamp(checkOutAt);

                dashboardTodayAttendanceStatus = safeText(status);
                dashboardCheckInTime = checkInText;
                dashboardCheckOutTime = checkOutText;

                String currentState = Boolean.TRUE.equals(checkedIn)
                        ? "Currently Checked In ✅"
                        : "Currently Checked Out / Not Active ❌";

                String text =
                        "Today's Attendance\n\n" +
                                "Date: " + safeText(date) + "\n" +
                                "Status: " + safeText(status) + "\n" +
                                "Current State: " + currentState + "\n" +
                                "Check In: " + checkInText + "\n" +
                                "Check Out: " + checkOutText + "\n" +
                                "Doc ID: " + getTodayAttendanceId();

                attendanceCardText.setText(text);
                renderDashboardSummary();
            } else {
                dashboardTodayAttendanceStatus = "not_started";
                dashboardCheckInTime = "-";
                dashboardCheckOutTime = "-";

                attendanceCardText.setText(
                        "Today's Attendance\n\n" +
                                "No attendance record yet for today.\n" +
                                "Manager can press Check In to create it.\n\n" +
                                "Expected Doc ID: " + getTodayAttendanceId()
                );

                renderDashboardSummary();
            }
        });

        attendanceHistoryListener = companyRef.collection("attendance")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        attendanceHistoryText.setText("Attendance history sync failed:\n" + error.getMessage());
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        renderAttendanceHistory(queryDocumentSnapshots);
                    }
                });

        tasksListener = companyRef.collection("tasks")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        taskSummaryText.setText("Task live sync failed:\n" + error.getMessage());
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        latestTasksSnapshot = queryDocumentSnapshots;
                        renderTaskList(queryDocumentSnapshots);
                        renderCurrentTaskFromSnapshot(queryDocumentSnapshots);

                        if (appContentBox.getVisibility() == View.VISIBLE) {
                            statusText.setText("Live task update received ✅");
                        }
                    }
                });
    }

    private void stopRealtimeListeners() {
        if (companyListener != null) companyListener.remove();
        if (managerListener != null) managerListener.remove();
        if (attendanceListener != null) attendanceListener.remove();
        if (attendanceHistoryListener != null) attendanceHistoryListener.remove();
        if (tasksListener != null) tasksListener.remove();

        companyListener = null;
        managerListener = null;
        attendanceListener = null;
        attendanceHistoryListener = null;
        tasksListener = null;
    }

    private void renderDashboardSummary() {
        String managerLiveText = dashboardManagerCheckedIn
                ? "Active / Checked In ✅"
                : "Not Active ❌";

        String viewText = isBossView ? "Boss Dashboard" : "Manager Dashboard";

        String summary =
                viewText + "\n\n" +
                        "Business: " + safeText(dashboardBusinessName) + "\n" +
                        "Manager: " + safeText(dashboardManagerName) + "\n" +
                        "Manager Status: " + managerLiveText + "\n\n" +
                        "Today's Attendance: " + safeText(dashboardTodayAttendanceStatus) + "\n" +
                        "Check In: " + safeText(dashboardCheckInTime) + "\n" +
                        "Check Out: " + safeText(dashboardCheckOutTime) + "\n\n" +
                        "Task Overview\n" +
                        "Pending: " + dashboardPendingTasks + "\n" +
                        "Completed: " + dashboardCompletedTasks + "\n" +
                        "Total: " + dashboardTotalTasks + "\n" +
                        "Filter: " + getTaskFilterLabel();

        dashboardSummaryText.setText(summary);
    }

    private void renderAttendanceHistory(QuerySnapshot queryDocumentSnapshots) {
        if (queryDocumentSnapshots.isEmpty()) {
            attendanceHistoryText.setText("Attendance History\n\nNo attendance records found.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Attendance History - Manager 1\n\n");

        int count = 0;

        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            String userId = document.getString("userId");

            if (!managerId.equals(userId)) {
                continue;
            }

            count++;

            String date = document.getString("date");
            String status = document.getString("status");
            Boolean checkedIn = document.getBoolean("checkedIn");
            Timestamp checkInAt = document.getTimestamp("checkInAt");
            Timestamp checkOutAt = document.getTimestamp("checkOutAt");

            String state = Boolean.TRUE.equals(checkedIn)
                    ? "Active / Checked In"
                    : "Inactive / Checked Out";

            builder.append(count)
                    .append(". Date: ")
                    .append(safeText(date))
                    .append("\nStatus: ")
                    .append(safeText(status))
                    .append("\nState: ")
                    .append(state)
                    .append("\nCheck In: ")
                    .append(formatTimestamp(checkInAt))
                    .append("\nCheck Out: ")
                    .append(formatTimestamp(checkOutAt))
                    .append("\nDoc ID: ")
                    .append(document.getId())
                    .append("\n\n");
        }

        if (count == 0) {
            attendanceHistoryText.setText("Attendance History\n\nNo attendance records found for Manager 1.");
        } else {
            attendanceHistoryText.setText(builder.toString());
        }
    }

    private void loadCurrentTaskOnce() {
        if (currentTaskRef == null) {
            currentTaskText.setText("Current Task\n\nNo current task selected.");
            return;
        }

        currentTaskRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        setCurrentTaskText(
                                currentTaskId,
                                documentSnapshot.getString("title"),
                                documentSnapshot.getString("description"),
                                documentSnapshot.getString("status"),
                                documentSnapshot.getString("assignedTo"),
                                documentSnapshot.getString("completionNote"),
                                documentSnapshot.getString("priority"),
                                documentSnapshot.getString("dueDateText"),
                                documentSnapshot.getString("attachmentFileName"),
                                documentSnapshot.getString("attachmentUrl")
                        );
                    } else {
                        currentTaskText.setText("Current Task\n\nCurrent task document not found.");
                    }
                })
                .addOnFailureListener(e -> currentTaskText.setText("Current task load failed:\n" + e.getMessage()));
    }

    private void renderCurrentTaskFromSnapshot(QuerySnapshot queryDocumentSnapshots) {
        boolean found = false;

        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            String taskId = document.getId();

            if (taskId.equals(currentTaskId)) {
                setCurrentTaskText(
                        taskId,
                        document.getString("title"),
                        document.getString("description"),
                        document.getString("status"),
                        document.getString("assignedTo"),
                        document.getString("completionNote"),
                        document.getString("priority"),
                        document.getString("dueDateText"),
                        document.getString("attachmentFileName"),
                        document.getString("attachmentUrl")
                );

                found = true;
                break;
            }
        }

        if (!found) {
            currentTaskText.setText("Current Task\n\nCurrent task not found in live task list.");
        }
    }

    private void setCurrentTaskText(
            String taskId,
            String title,
            String description,
            String status,
            String assignedTo,
            String completionNote,
            String priority,
            String dueDateText,
            String attachmentFileName,
            String attachmentUrl
    ) {
        String noteText = safeText(completionNote);
        if ("-".equals(noteText)) noteText = "No completion note yet";

        String attachmentText = safeText(attachmentFileName);
        if (!"-".equals(safeText(attachmentUrl))) {
            attachmentText = attachmentText + "\nAttachment URL: " + attachmentUrl;
        }

        String text =
                "Current Task\n\n" +
                        "Task ID: " + taskId + "\n" +
                        "Title: " + safeText(title) + "\n" +
                        "Description: " + safeText(description) + "\n" +
                        "Priority: " + safeText(priority) + "\n" +
                        "Due: " + safeText(dueDateText) + "\n" +
                        "Assigned To: " + safeText(assignedTo) + "\n" +
                        "Status: " + safeText(status) + "\n" +
                        "Note: " + noteText + "\n" +
                        "Attachment: " + attachmentText;

        currentTaskText.setText(text);
    }

    private void renderTaskList(QuerySnapshot queryDocumentSnapshots) {
        taskListContainer.removeAllViews();

        if (queryDocumentSnapshots.isEmpty()) {
            dashboardPendingTasks = 0;
            dashboardCompletedTasks = 0;
            dashboardTotalTasks = 0;
            renderDashboardSummary();
            taskSummaryText.setText("Task Summary\n\nNo tasks found yet.");
            return;
        }

        int pendingCount = 0;
        int completedCount = 0;
        int totalForManager = 0;
        int visibleCount = 0;

        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            String taskId = document.getId();
            String title = document.getString("title");
            String description = document.getString("description");
            String status = document.getString("status");
            String assignedTo = document.getString("assignedTo");
            String completionNote = document.getString("completionNote");
            String priority = document.getString("priority");
            String dueDateText = document.getString("dueDateText");
            String attachmentFileName = document.getString("attachmentFileName");
            String attachmentUrl = document.getString("attachmentUrl");

            if (!managerId.equals(assignedTo)) {
                continue;
            }

            totalForManager++;

            if ("completed".equals(status)) {
                completedCount++;
            } else {
                pendingCount++;
            }

            if (!shouldShowTask(status)) {
                continue;
            }

            visibleCount++;
            addTaskCard(
                    taskId,
                    title,
                    description,
                    status,
                    completionNote,
                    priority,
                    dueDateText,
                    attachmentFileName,
                    attachmentUrl
            );
        }

        dashboardPendingTasks = pendingCount;
        dashboardCompletedTasks = completedCount;
        dashboardTotalTasks = totalForManager;
        renderDashboardSummary();

        String summary =
                "Task Summary\n\n" +
                        "Pending: " + pendingCount + "\n" +
                        "Completed: " + completedCount + "\n" +
                        "Total: " + totalForManager + "\n" +
                        "Showing: " + getTaskFilterLabel() + "\n" +
                        "Visible in list: " + visibleCount;

        taskSummaryText.setText(summary);

        if (visibleCount == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No tasks match this filter.");
            emptyText.setTextSize(16);
            emptyText.setTextColor(Color.parseColor("#222222"));
            emptyText.setBackgroundColor(Color.WHITE);
            emptyText.setPadding(dp(16), dp(16), dp(16), dp(16));
            taskListContainer.addView(emptyText);
        }
    }

    private void addTaskCard(
            String taskId,
            String title,
            String description,
            String status,
            String completionNote,
            String priority,
            String dueDateText,
            String attachmentFileName,
            String attachmentUrl
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        TextView taskText = new TextView(this);
        taskText.setTextSize(16);
        taskText.setTextColor(Color.parseColor("#222222"));
        taskText.setLineSpacing(dp(4), 1.0f);

        String noteText = safeText(completionNote);
        if ("-".equals(noteText)) noteText = "No completion note yet";

        String attachmentText = safeText(attachmentFileName);
        if (!"-".equals(safeText(attachmentUrl))) {
            attachmentText = attachmentText + "\nAttachment URL: " + attachmentUrl;
        }

        String selectedFileText = "";
        if (selectedAttachmentNames.containsKey(taskId)) {
            selectedFileText = "\nSelected file: " + selectedAttachmentNames.get(taskId);
        }

        String displayText =
                "Task\n\n" +
                        "Title: " + safeText(title) + "\n" +
                        "Description: " + safeText(description) + "\n" +
                        "Priority: " + safeText(priority) + "\n" +
                        "Due: " + safeText(dueDateText) + "\n" +
                        "Status: " + safeText(status) + "\n" +
                        "ID: " + taskId + "\n" +
                        "Note: " + noteText + "\n" +
                        "Attachment: " + attachmentText +
                        selectedFileText;

        taskText.setText(displayText);
        card.addView(taskText);

        if (!"completed".equals(status)) {
            if (!isBossView) {
                EditText noteInput = new EditText(this);
                noteInput.setHint("Completion note for this task");
                noteInput.setMinLines(2);
                noteInput.setBackgroundColor(Color.parseColor("#F5F7FA"));
                noteInput.setPadding(dp(12), dp(12), dp(12), dp(12));

                LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                noteParams.setMargins(0, dp(12), 0, 0);
                noteInput.setLayoutParams(noteParams);
                card.addView(noteInput);

                Button attachButton = new Button(this);
                attachButton.setText("Attach File / Receipt");

                LinearLayout.LayoutParams attachParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                attachParams.setMargins(0, dp(10), 0, 0);
                attachButton.setLayoutParams(attachParams);

                attachButton.setOnClickListener(v -> chooseAttachmentForTask(taskId));
                card.addView(attachButton);

                Button completeButton = new Button(this);
                completeButton.setText("Manager Complete This Task");

                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                buttonParams.setMargins(0, dp(10), 0, 0);
                completeButton.setLayoutParams(buttonParams);

                completeButton.setOnClickListener(v -> {
                    String note = noteInput.getText().toString().trim();
                    completeTaskById(taskId, note);
                });

                card.addView(completeButton);
            } else {
                Button completeButton = new Button(this);
                completeButton.setText("Boss Mark This Task Completed");

                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                buttonParams.setMargins(0, dp(12), 0, 0);
                completeButton.setLayoutParams(buttonParams);

                completeButton.setOnClickListener(v -> completeTaskById(taskId, "Marked completed by Boss from Android app"));
                card.addView(completeButton);
            }
        }

        if (isBossView && "completed".equals(status)) {
            Button pendingButton = new Button(this);
            pendingButton.setText("Boss Reopen This Task");

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonParams.setMargins(0, dp(12), 0, 0);
            pendingButton.setLayoutParams(buttonParams);

            pendingButton.setOnClickListener(v -> reopenTaskById(taskId));
            card.addView(pendingButton);
        }

        taskListContainer.addView(card);
    }

    private void chooseAttachmentForTask(String taskId) {
        pendingAttachmentTaskId = taskId;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        filePickerLauncher.launch(intent);
    }

    private void bossAssignNewTask() {
        String title = taskTitleInput.getText().toString().trim();
        String description = taskDescriptionInput.getText().toString().trim();
        String priority = taskPrioritySpinner.getSelectedItem().toString();
        String dueDateText = taskDueDateInput.getText().toString().trim();

        if (title.isEmpty()) {
            statusText.setText("Enter a task title first.");
            return;
        }

        if (description.isEmpty()) {
            description = "No description added";
        }

        if (dueDateText.isEmpty()) {
            dueDateText = "No due date set";
        }

        statusText.setText("Assigning new task...");

        String newTaskId = "task_" + System.currentTimeMillis();

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("title", title);
        taskData.put("description", description);
        taskData.put("priority", priority);
        taskData.put("dueDateText", dueDateText);
        taskData.put("assignedTo", managerId);
        taskData.put("assignedBy", "boss_1");
        taskData.put("status", "pending");
        taskData.put("createdAt", FieldValue.serverTimestamp());

        DocumentReference newTaskRef = companyRef.collection("tasks").document(newTaskId);

        newTaskRef.set(taskData)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> companyUpdates = new HashMap<>();
                    companyUpdates.put("currentTaskId", newTaskId);
                    companyUpdates.put("updatedAt", FieldValue.serverTimestamp());

                    companyRef.update(companyUpdates)
                            .addOnSuccessListener(unused2 -> {
                                currentTaskId = newTaskId;
                                currentTaskRef = newTaskRef;

                                taskTitleInput.setText("");
                                taskDescriptionInput.setText("");
                                taskPrioritySpinner.setSelection(0);
                                taskDueDateInput.setText("");

                                statusText.setText("New task assigned to Manager 1 ✅");
                            })
                            .addOnFailureListener(e -> statusText.setText("Company update failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> statusText.setText("Task assign failed: " + e.getMessage()));
    }

    private void managerCheckIn() {
        statusText.setText("Checking in...");

        Map<String, Object> managerUpdates = new HashMap<>();
        managerUpdates.put("checkedIn", true);
        managerUpdates.put("lastCheckInAt", FieldValue.serverTimestamp());

        managerRef.update(managerUpdates)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> attendanceData = new HashMap<>();
                    attendanceData.put("userId", managerId);
                    attendanceData.put("date", getTodayDateText());
                    attendanceData.put("checkedIn", true);
                    attendanceData.put("status", "checked_in");
                    attendanceData.put("checkInAt", FieldValue.serverTimestamp());
                    attendanceData.put("updatedAt", FieldValue.serverTimestamp());

                    attendanceRef.set(attendanceData, SetOptions.merge())
                            .addOnSuccessListener(unused2 -> statusText.setText("Manager checked in ✅"))
                            .addOnFailureListener(e -> statusText.setText("Attendance check-in failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> statusText.setText("Manager check-in failed: " + e.getMessage()));
    }

    private void managerCheckOut() {
        statusText.setText("Checking out...");

        Map<String, Object> managerUpdates = new HashMap<>();
        managerUpdates.put("checkedIn", false);
        managerUpdates.put("lastCheckOutAt", FieldValue.serverTimestamp());

        managerRef.update(managerUpdates)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> attendanceData = new HashMap<>();
                    attendanceData.put("userId", managerId);
                    attendanceData.put("date", getTodayDateText());
                    attendanceData.put("checkedIn", false);
                    attendanceData.put("status", "checked_out");
                    attendanceData.put("checkOutAt", FieldValue.serverTimestamp());
                    attendanceData.put("updatedAt", FieldValue.serverTimestamp());

                    attendanceRef.set(attendanceData, SetOptions.merge())
                            .addOnSuccessListener(unused2 -> statusText.setText("Manager checked out ✅"))
                            .addOnFailureListener(e -> statusText.setText("Attendance check-out failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> statusText.setText("Manager check-out failed: " + e.getMessage()));
    }

    private void completeTaskById(String taskId, String note) {
        statusText.setText("Completing task...");

        DocumentReference taskRef = companyRef.collection("tasks").document(taskId);

        String finalNote = note;
        if (finalNote == null || finalNote.trim().isEmpty()) {
            finalNote = isBossView
                    ? "Marked completed by Boss from Android app"
                    : "Completed by Manager 1 from Android app";
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "completed");
        updates.put("completedAt", FieldValue.serverTimestamp());
        updates.put("completionNote", finalNote);

        Uri attachmentUri = selectedAttachmentUris.get(taskId);

        if (!isBossView && attachmentUri != null) {
            uploadAttachmentThenComplete(taskId, taskRef, updates, attachmentUri);
        } else {
            completeTaskWithUpdates(taskId, taskRef, updates);
        }
    }

    private void uploadAttachmentThenComplete(
            String taskId,
            DocumentReference taskRef,
            Map<String, Object> updates,
            Uri attachmentUri
    ) {
        String fileName = selectedAttachmentNames.containsKey(taskId)
                ? selectedAttachmentNames.get(taskId)
                : "attachment";

        String safeFileName = sanitizeFileName(fileName);
        String storagePath = "companies/global_plaza/tasks/" + taskId + "/attachments/" +
                System.currentTimeMillis() + "_" + safeFileName;

        StorageReference fileRef = storage.getReference().child(storagePath);

        statusText.setText("Uploading attachment...");

        fileRef.putFile(attachmentUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            updates.put("attachmentFileName", fileName);
                            updates.put("attachmentPath", storagePath);
                            updates.put("attachmentUrl", downloadUri.toString());
                            updates.put("attachmentUploadedAt", FieldValue.serverTimestamp());

                            completeTaskWithUpdates(taskId, taskRef, updates);
                        })
                        .addOnFailureListener(e -> statusText.setText("Download URL failed: " + e.getMessage())))
                .addOnFailureListener(e -> statusText.setText("Attachment upload failed: " + e.getMessage()));
    }

    private void completeTaskWithUpdates(String taskId, DocumentReference taskRef, Map<String, Object> updates) {
        taskRef.update(updates)
                .addOnSuccessListener(unused -> {
                    selectedAttachmentUris.remove(taskId);
                    selectedAttachmentNames.remove(taskId);
                    statusText.setText("Task completed ✅ Live sync will update list.");
                })
                .addOnFailureListener(e -> statusText.setText("Task completion failed: " + e.getMessage()));
    }

    private void reopenTaskById(String taskId) {
        statusText.setText("Reopening task...");

        DocumentReference taskRef = companyRef.collection("tasks").document(taskId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "pending");
        updates.put("completionNote", FieldValue.delete());
        updates.put("completedAt", FieldValue.delete());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        taskRef.update(updates)
                .addOnSuccessListener(unused -> statusText.setText("Task reopened ✅ Live sync will update list."))
                .addOnFailureListener(e -> statusText.setText("Task reopen failed: " + e.getMessage()));
    }

    private void bossSetCurrentTaskPending() {
        if (currentTaskRef == null) {
            statusText.setText("No current task found.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "pending");
        updates.put("assignedTo", managerId);
        updates.put("assignedBy", "boss_1");
        updates.put("completionNote", FieldValue.delete());
        updates.put("completedAt", FieldValue.delete());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        currentTaskRef.update(updates)
                .addOnSuccessListener(unused -> statusText.setText("Boss set current task to pending ✅"))
                .addOnFailureListener(e -> statusText.setText("Boss update failed: " + e.getMessage()));
    }

    private boolean shouldShowTask(String status) {
        if ("pending".equals(taskFilter)) {
            return !"completed".equals(status);
        }

        if ("completed".equals(taskFilter)) {
            return "completed".equals(status);
        }

        return true;
    }

    private String getTaskFilterLabel() {
        if ("pending".equals(taskFilter)) return "Pending only";
        if ("completed".equals(taskFilter)) return "Completed only";
        return "All tasks";
    }

    private String getTodayAttendanceId() {
        return "attendance_" + getTodayIdText() + "_" + managerId;
    }

    private String getTodayIdText() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getTodayDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "-";

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    private String getFileNameFromUri(Uri uri) {
        String result = "attachment";

        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            try {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    result = cursor.getString(nameIndex);
                }
            } finally {
                cursor.close();
            }
        }

        return result;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "attachment";
        }

        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String safeText(String value) {
        if (value == null || value.isEmpty()) return "-";
        return value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
package com.serhio.homeaccountingapp

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.serhio.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startActivity
import android.Manifest
import android.app.AlertDialog
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class TaskActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("tasks_prefs", Context.MODE_PRIVATE)

        val viewModel: TaskViewModel by viewModels {
            TaskViewModelFactory(sharedPreferences, gson, this)
        }

        setContent {
            HomeAccountingAppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var showOverdueMessage by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.loadTasks() // Завантаження задач
                    if (viewModel.hasOverdueTasks()) { // Перевірка прострочених задач
                        delay(500) // Затримка перед показом повідомлення
                        showOverdueMessage = true
                        delay(5000) // Затримка на 5 секунд
                        showOverdueMessage = false
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNavigateToMainActivity = {
                                val intent = Intent(this@TaskActivity, MainActivity::class.java).apply {
                                    putExtra("SHOW_SPLASH_SCREEN", false)
                                }
                                startActivity(intent)
                            },
                            onNavigateToIncomes = { navigateToActivity(IncomeActivity::class.java) },
                            onNavigateToExpenses = { navigateToActivity(ExpenseActivity::class.java) },
                            onNavigateToIssuedOnLoan = { navigateToActivity(IssuedOnLoanActivity::class.java) },
                            onNavigateToBorrowed = { navigateToActivity(BorrowedActivity::class.java) },
                            onNavigateToAllTransactionIncome = { navigateToActivity(AllTransactionIncomeActivity::class.java) },
                            onNavigateToAllTransactionExpense = { navigateToActivity(AllTransactionExpenseActivity::class.java) },
                            onNavigateToBudgetPlanning = { navigateToActivity(BudgetPlanningActivity::class.java) },
                            onNavigateToTaskActivity = { navigateToActivity(TaskActivity::class.java) }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Задачник", color = Color.White) },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Меню", tint = Color.White)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                            )
                        },
                        content = { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .paint(
                                        painter = painterResource(id = R.drawable.background_app),
                                        contentScale = ContentScale.Crop
                                    )
                                    .padding(innerPadding)
                            ) {
                                TaskScreen(viewModel)

                                // Анімоване повідомлення про прострочені завдання
                                AnimatedVisibility(
                                    visible = showOverdueMessage,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 100.dp) // збільшено значення відступу
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f) // встановлено ширину на 90% від ширини екрану
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    listOf(Color.Yellow.copy(alpha = 0.8f), Color.Transparent)
                                                ),
                                                shape = RoundedCornerShape(16.dp) // додано зглажені кути
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Text("У вас є невиконане завдання", color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        checkNotificationPermission()
        checkExactAlarmPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog(Manifest.permission.POST_NOTIFICATIONS, REQUEST_CODE_NOTIFICATION_PERMISSION)
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivityForResult(intent, REQUEST_CODE_EXACT_ALARM_PERMISSION)
            }
        }
    }

    private fun showPermissionDialog(permission: String, requestCode: Int) {
        AlertDialog.Builder(this)
            .setTitle("Дозвіл на сповіщення")
            .setMessage("Цей додаток потребує дозволу на сповіщення. Будь ласка, надайте дозвіл.")
            .setPositiveButton("Дозволити") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    requestCode
                )
            }
            .setNegativeButton("Відмінити", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, you can show notifications
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Будь ласка, увімкніть дозволи на повідомлення у налаштуваннях додатка", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1
        private const val REQUEST_CODE_EXACT_ALARM_PERMISSION = 2
    }
}

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val startDate: Date,
    val endDate: Date,
    var isCompleted: Boolean = false,
    var reminder: String? = null // New field for reminder
)

class TaskViewModel(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    private val context: Context
) : ViewModel() {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _tasks

    fun addTask(task: Task) {
        _tasks.add(task)
        saveTasks()
    }

    fun updateTask(updatedTask: Task) {
        val index = _tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            _tasks[index] = updatedTask
            saveTasks()
        }
    }

    private fun cancelTaskReminders(task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val startPendingIntent = PendingIntent.getBroadcast(context, task.id.hashCode(), Intent(context, ReminderBroadcastReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)
        val endPendingIntent = PendingIntent.getBroadcast(context, task.id.hashCode() + 1, Intent(context, ReminderBroadcastReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(startPendingIntent)
        alarmManager.cancel(endPendingIntent)
    }

    fun removeTask(task: Task) {
        // Скасування нагадувань
        cancelTaskReminders(task)
        _tasks.remove(task)
        saveTasks()
    }

    fun toggleTaskCompletion(task: Task) {
        val index = _tasks.indexOf(task)
        if (index != -1) {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            _tasks[index] = updatedTask

            // Скасування нагадувань, якщо задача відзначена як виконана
            if (updatedTask.isCompleted) {
                cancelTaskReminders(updatedTask)
            }

            saveTasks()
        }
    }

    // Завантаження задач з SharedPreferences
    fun loadTasks() {
        val tasksJson = sharedPreferences.getString("tasks", "[]")
        val type = object : TypeToken<List<Task>>() {}.type
        val loadedTasks: List<Task> = gson.fromJson(tasksJson, type)
        _tasks.clear()
        _tasks.addAll(loadedTasks)
    }

    // Збереження задач у SharedPreferences
    private fun saveTasks() {
        val editor = sharedPreferences.edit()
        val tasksJson = gson.toJson(_tasks)
        editor.putString("tasks", tasksJson)
        editor.apply()
    }

    // Перевірка наявності прострочених завдань
    fun hasOverdueTasks(): Boolean {
        val currentDate = Date()
        return _tasks.any { !it.isCompleted && it.endDate.before(currentDate) }
    }
}

class TaskViewModelFactory(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(sharedPreferences, gson, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showSaveMessage by remember { mutableStateOf(false) }
    var showReminderMessage by remember { mutableStateOf(false) }
    var reminderTaskTitle by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = Color(0xFF228B22),
            ) {
                Text("+", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .paint(
                        painter = painterResource(id = R.drawable.background_app),
                        contentScale = ContentScale.Crop
                    )
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(bottom = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TaskList(viewModel.tasks, viewModel::toggleTaskCompletion, viewModel::removeTask, onEditTask = { task ->
                        editingTask = task
                        showAddTaskDialog = true
                    })
                }

                if (showAddTaskDialog) {
                    AddTaskDialog(
                        taskToEdit = editingTask,
                        onDismiss = {
                            showAddTaskDialog = false
                            editingTask = null
                        },
                        onSave = { task ->
                            if (editingTask != null) {
                                viewModel.updateTask(task)
                            } else {
                                viewModel.addTask(task)
                            }

                            showAddTaskDialog = false
                            editingTask = null
                            showSaveMessage = true

                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                            // Schedule reminder for task start time
                            scheduleReminder(alarmManager, context, task.startDate.time, task.title, "START", task.id.hashCode(), "на початку")
                        }
                    )
                }

                if (showSaveMessage) {
                    LaunchedEffect(Unit) {
                        delay(3000)
                        showSaveMessage = false
                    }

                    AnimatedVisibility(
                        visible = showSaveMessage,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(500)),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(500)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFF004d00).copy(alpha = 0.8f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text("Задача збережена", color = Color.White)
                        }
                    }
                }

                if (showReminderMessage) {
                    LaunchedEffect(Unit) {
                        delay(3000)
                        showReminderMessage = false
                    }

                    AnimatedVisibility(
                        visible = showReminderMessage,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(500)),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(500)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFFFFF00).copy(alpha = 0.8f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text("Нагадування по задачі \"$reminderTaskTitle\"", color = Color.Black)
                        }
                    }
                }
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("ScheduleExactAlarm")
fun scheduleReminder(alarmManager: AlarmManager, context: Context, triggerAtMillis: Long, taskTitle: String, action: String, requestCode: Int, reminderTime: String) {
    val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
        putExtra("TASK_TITLE", taskTitle)
        putExtra("ACTION", action)
        putExtra("REMINDER_TIME", reminderTime)
    }
    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("TASK_TITLE")
        val action = intent.getStringExtra("ACTION")
        val reminderTime = intent.getStringExtra("REMINDER_TIME")

        val message = when (action) {
            "START" -> taskTitle ?: "Задача"
            "REMINDER" -> "Задача \"$taskTitle\" почнеться через $reminderTime"
            else -> "Нагадування по задачі \"$taskTitle\""
        }

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            showNotification(context, message)
            vibratePhone(context)
        } else {
            requestNotificationPermission(context)
        }
    }

    private fun showNotification(context: Context, message: String?) {
        val channelId = "task_reminder_channel"
        val channelName = "Task Reminder"
        val importance = NotificationManager.IMPORTANCE_HIGH

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Канал для нагадувань про задачі"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, TaskActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Нагадування")
            .setContentText(message ?: "Нагадування")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                with(NotificationManagerCompat.from(context)) {
                    notify(System.currentTimeMillis().toInt(), builder.build())
                }
            } catch (e: SecurityException) {
                requestNotificationPermission(context)
            }
        } else {
            requestNotificationPermission(context)
        }
    }

    private fun vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(500)
        }
    }

    private fun requestNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Будь ласка, увімкніть дозволи на повідомлення у налаштуваннях додатка", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun TaskList(
    tasks: List<Task>,
    onToggleCompletion: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit // Add this parameter for editing a task
) {
    LazyColumn {
        items(tasks) { task ->
            TaskItem(task, onToggleCompletion, onDeleteTask, onEditTask) // Pass the onEditTask callback
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggleCompletion: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit // Add this parameter for editing a task
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1E1E1E).copy(alpha = 0.8f)) // Яскравіше, але прозоре
            .padding(16.dp)
            .clickable { onEditTask(task) }, // Add clickable modifier to trigger edit
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold, // Жирний шрифт
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = Color.White
                )
            )
            task.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = Color.White
                    )
                )
            }
            Text(
                text = "Початок: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(task.startDate)}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
            )

            if (task.isCompleted) {
                Text(
                    text = "Виконано",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Green)
                )
            }
        }
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggleCompletion(task) },
            colors = CheckboxDefaults.colors(checkedColor = Color.Green)
        )
        IconButton(onClick = { onDeleteTask(task) }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Видалити", tint = Color.White)
        }
    }
}
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    taskToEdit: Task? = null,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var startDate by remember { mutableStateOf(taskToEdit?.startDate ?: Date()) }
    var startTime by remember { mutableStateOf(taskToEdit?.startDate ?: Date()) }
    var endDate by remember { mutableStateOf(taskToEdit?.endDate ?: Date()) }
    var endTime by remember { mutableStateOf(taskToEdit?.endDate ?: Date()) }
    var reminder by remember { mutableStateOf(taskToEdit?.reminder ?: "За 10 хвилин") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showReminderMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (showStartDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                startDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time
                showStartDatePicker = false
                showStartTimePicker = true
            },
            startDate.year + 1900,
            startDate.month,
            startDate.date
        ).show()
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                startTime = Calendar.getInstance().apply {
                    time = startDate
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }.time
                showStartTimePicker = false
            },
            startTime.hours,
            startTime.minutes,
            true
        ).show()
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                endDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time
                showEndDatePicker = false
                showEndTimePicker = true
            },
            endDate.year + 1900,
            endDate.month,
            endDate.date
        ).show()
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                endTime = Calendar.getInstance().apply {
                    time = endDate
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }.time
                showEndTimePicker = false
            },
            endTime.hours,
            endTime.minutes,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(if (taskToEdit == null) "Додати задачу" else "Редагувати задачу", color = Color.White)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва задачі", color = Color.White) },
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Опис задачі", color = Color.White) },
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    singleLine = false,
                    maxLines = 10
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFF4CAF50)), shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = "Початок: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(startTime)}",
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showReminderMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color.Yellow), shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = "Нагадування: $reminder",
                        color = Color.Yellow,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                DropdownMenu(
                    expanded = showReminderMenu,
                    onDismissRequest = { showReminderMenu = false },
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.4f))
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    DropdownMenuItem(
                        onClick = {
                            reminder = "Не нагадувати"
                            showReminderMenu = false
                        },
                        text = { Text("Не нагадувати", color = Color.White) }
                    )
                    DropdownMenuItem(
                        onClick = {
                            reminder = "За 10 хвилин"
                            showReminderMenu = false
                        },
                        text = { Text("За 10 хвилин", color = Color.White) }
                    )
                    DropdownMenuItem(
                        onClick = {
                            reminder = "За пів години"
                            showReminderMenu = false
                        },
                        text = { Text("За пів години", color = Color.White) }
                    )
                    DropdownMenuItem(
                        onClick = {
                            reminder = "За годину"
                            showReminderMenu = false
                        },
                        text = { Text("За годину", color = Color.White) }
                    )
                    DropdownMenuItem(
                        onClick = {
                            reminder = "За день"
                            showReminderMenu = false
                        },
                        text = { Text("За день", color = Color.White) }
                    )
                    DropdownMenuItem(
                        onClick = {
                            reminder = "За тиждень"
                            showReminderMenu = false
                        },
                        text = { Text("За тиждень", color = Color.White) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        val task = Task(
                            taskToEdit?.id ?: UUID.randomUUID().toString(),
                            title,
                            description.ifEmpty { null },
                            startTime,
                            endTime,
                            reminder = reminder
                        )
                        onSave(task)
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val reminderTime = when (reminder) {
                            "За 10 хвилин" -> "10 хвилин"
                            "За пів години" -> "пів години"
                            "За годину" -> "годину"
                            "За день" -> "день"
                            "За тиждень" -> "тиждень"
                            else -> ""
                        }

                        // Schedule reminder based on selected option
                        when (reminder) {
                            "За 10 хвилин" -> scheduleReminder(alarmManager, context, task.startDate.time - 10 * 60 * 1000, task.title, "REMINDER", task.id.hashCode(), reminderTime)
                            "За пів години" -> scheduleReminder(alarmManager, context, task.startDate.time - 30 * 60 * 1000, task.title, "REMINDER", task.id.hashCode(), reminderTime)
                            "За годину" -> scheduleReminder(alarmManager, context, task.startDate.time - 60 * 60 * 1000, task.title, "REMINDER", task.id.hashCode(), reminderTime)
                            "За день" -> scheduleReminder(alarmManager, context, task.startDate.time - 24 * 60 * 60 * 1000, task.title, "REMINDER", task.id.hashCode(), reminderTime)
                            "За тиждень" -> scheduleReminder(alarmManager, context, task.startDate.time - 7 * 24 * 60 * 60 * 1000, task.title, "REMINDER", task.id.hashCode(), reminderTime)
                        }
                    } else {
                        // Show error message
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Зберегти", color = Color.Green)
            }
        },
        dismissButton = {
            Button(
                onClick = { onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Відмінити", color = Color.Red)
            }
        },
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Gray.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.8f))
                ),
                shape = MaterialTheme.shapes.medium
            )
            .border(BorderStroke(1.dp, Color.White)),
        containerColor = Color.Transparent,
        textContentColor = Color.White
    )
}

package iMel9i.garminhud.lite

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppSettingsActivity : AppCompatActivity() {
    
    private lateinit var configManager: AppConfigManager
    private lateinit var adapter: AppConfigAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        
        title = "Настройки приложений"
        
        configManager = AppConfigManager(this)
        
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerApps)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = AppConfigAdapter(
            configs = configManager.getConfigs().toMutableList(),
            onToggle = { config, enabled ->
                val updated = config.copy(enabled = enabled)
                configManager.updateConfig(config.packageName, updated)
            },
            onEdit = { config ->
                showEditDialog(config)
            },
            onDelete = { config ->
                AlertDialog.Builder(this)
                    .setTitle("Удалить приложение?")
                    .setMessage("Удалить ${config.appName} из списка?")
                    .setPositiveButton("Удалить") { _, _ ->
                        configManager.deleteConfig(config.packageName)
                        adapter.removeConfig(config)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        )
        
        recyclerView.adapter = adapter
        
        findViewById<Button>(R.id.btnAddApp).setOnClickListener {
            showEditDialog(null)
        }
    }
    
    private fun showEditDialog(config: AppConfigManager.AppConfig?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_app, null)
        
        val editAppName = dialogView.findViewById<EditText>(R.id.editAppName)
        val editPackageName = dialogView.findViewById<EditText>(R.id.editPackageName)
        val layoutFields = dialogView.findViewById<LinearLayout>(R.id.layoutFields)
        val btnAddField = dialogView.findViewById<Button>(R.id.btnAddField)
        
        // Hide "Add Field" button as we use fixed list
        btnAddField.visibility = View.GONE
        
        val fields = mutableMapOf<String, String>()
        
        // Pre-fill if editing
        config?.let {
            editAppName.setText(it.appName)
            editPackageName.setText(it.packageName)
            editPackageName.isEnabled = false
            fields.putAll(it.fields)
        }
        
        // Variables to map
        val variables = listOf(
            HudDataType.DISTANCE_TO_TURN to "Дистанция",
            HudDataType.ETA to "Время прибытия (ETA)",
            HudDataType.REMAINING_TIME to "Время в пути",
            HudDataType.TRAFFIC_SCORE to "Баллы пробок",
            HudDataType.NAVIGATION_INSTRUCTION to "Инструкция (Текст)",
            HudDataType.SPEED_LIMIT to "Ограничение скорости",
            HudDataType.CURRENT_SPEED to "Текущая скорость"
        )
        
        // Populate rows
        variables.forEach { (type, label) ->
            val key = type.name
            val value = fields[key] ?: ""
            addFieldRow(layoutFields, label, key, value, fields)
        }
        
        AlertDialog.Builder(this)
            .setTitle(if (config == null) "Добавить приложение" else "Изменить приложение")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val appName = editAppName.text.toString()
                val packageName = editPackageName.text.toString()
                
                if (appName.isBlank() || packageName.isBlank()) {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val newConfig = AppConfigManager.AppConfig(
                    packageName = packageName,
                    appName = appName,
                    enabled = config?.enabled ?: true,
                    fields = fields.toMap()
                )
                
                if (config == null) {
                    configManager.addConfig(newConfig)
                    adapter.addConfig(newConfig)
                } else {
                    configManager.updateConfig(packageName, newConfig)
                    adapter.updateConfig(newConfig)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun addFieldRow(
        container: LinearLayout,
        label: String,
        key: String,
        initialValue: String,
        fields: MutableMap<String, String>
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }
        
        val textLabel = TextView(this).apply {
            text = label
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 4
            }
        }
        
        val editValue = EditText(this).apply {
            hint = "ID поля (например android.title)"
            setText(initialValue)
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        editValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = editValue.text.toString()
                if (value.isNotBlank()) {
                    fields[key] = value
                } else {
                    fields.remove(key)
                }
            }
        }
        
        // Also save on text change to be safe
        editValue.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val value = s.toString()
                if (value.isNotBlank()) {
                    fields[key] = value
                } else {
                    fields.remove(key)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        row.addView(textLabel)
        row.addView(editValue)
        
        container.addView(row)
    }
}

class AppConfigAdapter(
    private val configs: MutableList<AppConfigManager.AppConfig>,
    private val onToggle: (AppConfigManager.AppConfig, Boolean) -> Unit,
    private val onEdit: (AppConfigManager.AppConfig) -> Unit,
    private val onDelete: (AppConfigManager.AppConfig) -> Unit
) : RecyclerView.Adapter<AppConfigAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val switchEnabled: Switch = view.findViewById(R.id.switchEnabled)
        val textAppName: TextView = view.findViewById(R.id.textAppName)
        val textPackageName: TextView = view.findViewById(R.id.textPackageName)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
        val layoutFields: LinearLayout = view.findViewById(R.id.layoutFields)
        val textFields: TextView = view.findViewById(R.id.textFields)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_config, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val config = configs[position]
        
        holder.textAppName.text = config.appName
        holder.textPackageName.text = config.packageName
        holder.switchEnabled.isChecked = config.enabled
        
        holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            onToggle(config, isChecked)
        }
        
        holder.btnEdit.setOnClickListener {
            onEdit(config)
        }
        
        holder.btnDelete.setOnClickListener {
            onDelete(config)
        }
        
        // Show fields
        val fieldsText = config.fields.entries.joinToString("\n") { (key, value) ->
            "$key: $value"
        }
        holder.textFields.text = fieldsText
        
        // Toggle fields visibility on click
        holder.itemView.setOnClickListener {
            holder.layoutFields.visibility = if (holder.layoutFields.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }
    
    override fun getItemCount() = configs.size
    
    fun addConfig(config: AppConfigManager.AppConfig) {
        configs.add(config)
        notifyItemInserted(configs.size - 1)
    }
    
    fun updateConfig(config: AppConfigManager.AppConfig) {
        val index = configs.indexOfFirst { it.packageName == config.packageName }
        if (index != -1) {
            configs[index] = config
            notifyItemChanged(index)
        }
    }
    
    fun removeConfig(config: AppConfigManager.AppConfig) {
        val index = configs.indexOfFirst { it.packageName == config.packageName }
        if (index != -1) {
            configs.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}

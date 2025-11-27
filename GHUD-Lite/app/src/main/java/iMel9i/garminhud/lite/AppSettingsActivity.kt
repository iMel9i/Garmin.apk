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
        
        val fields = mutableMapOf<String, String>()
        
        // Pre-fill if editing
        config?.let {
            editAppName.setText(it.appName)
            editPackageName.setText(it.packageName)
            editPackageName.isEnabled = false // Don't allow changing package name when editing
            fields.putAll(it.fields)
        }
        
        // Add default fields if new
        if (config == null) {
            fields["title"] = "android.title"
            fields["text"] = "android.text"
            fields["bigText"] = "android.bigText"
        }
        
        fun refreshFields() {
            layoutFields.removeAllViews()
            fields.forEach { (fieldName, fieldValue) ->
                addFieldRow(layoutFields, fieldName, fieldValue, fields)
            }
        }
        
        refreshFields()
        
        btnAddField.setOnClickListener {
            val newFieldName = "field${fields.size + 1}"
            fields[newFieldName] = "android.custom"
            refreshFields()
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
        fieldName: String,
        fieldValue: String,
        fields: MutableMap<String, String>
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }
        
        val editName = EditText(this).apply {
            hint = "Имя поля"
            setText(fieldName)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val editValue = EditText(this).apply {
            hint = "Ключ (android.title)"
            setText(fieldValue)
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f).apply {
                marginStart = 8
            }
        }
        
        val btnRemove = Button(this).apply {
            text = "×"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8
            }
            setOnClickListener {
                fields.remove(fieldName)
                container.removeView(row)
            }
        }
        
        // Update map when text changes
        editName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val oldName = fieldName
                val newName = editName.text.toString()
                if (newName != oldName && newName.isNotBlank()) {
                    fields.remove(oldName)
                    fields[newName] = editValue.text.toString()
                }
            }
        }
        
        editValue.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                fields[editName.text.toString()] = editValue.text.toString()
            }
        }
        
        row.addView(editName)
        row.addView(editValue)
        row.addView(btnRemove)
        
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

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

class OsmSettingsActivity : AppCompatActivity() {
    
    private lateinit var configManager: OsmConfigManager
    private lateinit var adapter: OsmConfigAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_osm_settings)
        
        title = "Настройки OpenStreetMap"
        
        configManager = OsmConfigManager(this)
        
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerOsmApis)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = OsmConfigAdapter(
            configs = configManager.getConfigs().toMutableList(),
            onToggle = { config, enabled ->
                val updated = config.copy(enabled = enabled)
                configManager.updateConfig(config.dataType, updated)
            },
            onEdit = { config ->
                showEditDialog(config)
            }
        )
        
        recyclerView.adapter = adapter
    }
    
    private fun showEditDialog(config: OsmConfigManager.OsmApiConfig) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_osm, null)
        
        val textDataType = dialogView.findViewById<TextView>(R.id.textDataType)
        val editApiUrl = dialogView.findViewById<EditText>(R.id.editApiUrl)
        val editQuery = dialogView.findViewById<EditText>(R.id.editQuery)
        val editSearchRadius = dialogView.findViewById<EditText>(R.id.editSearchRadius)
        val editParseField = dialogView.findViewById<EditText>(R.id.editParseField)
        
        textDataType.text = "Переменная: ${config.dataType.displayName}"
        editApiUrl.setText(config.apiUrl)
        editQuery.setText(config.query)
        editSearchRadius.setText(config.searchRadius.toString())
        editParseField.setText(config.parseField)
        
        AlertDialog.Builder(this)
            .setTitle("Настройки OSM API")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val updated = config.copy(
                    apiUrl = editApiUrl.text.toString(),
                    query = editQuery.text.toString(),
                    searchRadius = editSearchRadius.text.toString().toIntOrNull() ?: 20,
                    parseField = editParseField.text.toString()
                )
                
                configManager.updateConfig(config.dataType, updated)
                adapter.updateConfig(updated)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}

class OsmConfigAdapter(
    private val configs: MutableList<OsmConfigManager.OsmApiConfig>,
    private val onToggle: (OsmConfigManager.OsmApiConfig, Boolean) -> Unit,
    private val onEdit: (OsmConfigManager.OsmApiConfig) -> Unit
) : RecyclerView.Adapter<OsmConfigAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val switchEnabled: Switch = view.findViewById(R.id.switchOsmEnabled)
        val textDataType: TextView = view.findViewById(R.id.textOsmDataType)
        val textApiUrl: TextView = view.findViewById(R.id.textOsmApiUrl)
        val btnEdit: Button = view.findViewById(R.id.btnOsmEdit)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_osm_config, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val config = configs[position]
        
        holder.textDataType.text = config.dataType.displayName
        holder.textApiUrl.text = "API: ${config.apiUrl}"
        holder.switchEnabled.isChecked = config.enabled
        
        holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            onToggle(config, isChecked)
        }
        
        holder.btnEdit.setOnClickListener {
            onEdit(config)
        }
    }
    
    override fun getItemCount() = configs.size
    
    fun updateConfig(config: OsmConfigManager.OsmApiConfig) {
        val index = configs.indexOfFirst { it.dataType == config.dataType }
        if (index != -1) {
            configs[index] = config
            notifyItemChanged(index)
        }
    }
}

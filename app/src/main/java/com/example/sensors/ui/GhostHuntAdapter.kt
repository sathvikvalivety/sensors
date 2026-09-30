package com.example.sensors.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sensors.databinding.ItemHuntHistoryBinding
import com.example.sensors.model.GhostHuntRecord
import java.text.SimpleDateFormat
import java.util.*

class GhostHuntAdapter(
    private var hunts: List<GhostHuntRecord>,
    private val onDeleteClicked: (GhostHuntRecord) -> Unit
) : RecyclerView.Adapter<GhostHuntAdapter.HuntViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    fun updateData(newHunts: List<GhostHuntRecord>) {
        this.hunts = newHunts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HuntViewHolder {
        val binding = ItemHuntHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HuntViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HuntViewHolder, position: Int) {
        holder.bind(hunts[position])
    }

    override fun getItemCount(): Int = hunts.size

    inner class HuntViewHolder(private val binding: ItemHuntHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(hunt: GhostHuntRecord) {
            binding.tvHistoryGhostName.text = "👻 ${hunt.ghostName}"
            binding.tvHistoryPeakScore.text = "${hunt.peakScore}% PEAK"
            binding.tvHistoryMood.text = "Mood: ${hunt.ghostMood}"
            binding.tvHistoryMeta.text = "📅 ${dateFormat.format(Date(hunt.timestamp))} • ⏱️ ${hunt.durationFormatted} • 📍 ${hunt.locationTag}"
            binding.tvHistorySensors.text = hunt.sensorBreakdown
            binding.btnDeleteHistory.setOnClickListener {
                onDeleteClicked(hunt)
            }
        }
    }
}

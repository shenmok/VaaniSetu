package com.example.vaanisetu.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vaanisetu.R

data class PeerInfo(
    val endpointId: String,
    val displayName: String
)

class PeerAdapter(
    private val onCallClick: (PeerInfo) -> Unit
) : RecyclerView.Adapter<PeerAdapter.PeerViewHolder>() {

    private val peers = mutableListOf<PeerInfo>()

    fun updatePeers(newPeers: List<PeerInfo>) {
        peers.clear()
        peers.addAll(newPeers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_peer, parent, false)
        return PeerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        holder.bind(peers[position])
    }

    override fun getItemCount(): Int = peers.size

    inner class PeerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val peerName: TextView = itemView.findViewById(R.id.peerName)
        private val btnCall: ImageButton = itemView.findViewById(R.id.btnCallPeer)

        fun bind(peer: PeerInfo) {
            peerName.text = peer.displayName
            btnCall.setOnClickListener { onCallClick(peer) }
            itemView.setOnClickListener { onCallClick(peer) }
        }
    }
}

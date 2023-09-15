package io.agora.board.fast.sample.cases.hione

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import io.agora.board.fast.sample.R

class HiOneColorAdapter(
    private val colors: IntArray
) : RecyclerView.Adapter<HiOneColorAdapter.ViewHolder>() {

    var curColor: Int = 0
    var onColorClickListener: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.hione_item_tool_color, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val color = colors[position]
        holder.bind(color)
    }

    override fun getItemCount() = colors.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val colorDisplay = itemView.findViewById<HiOneColorView>(R.id.color_display)

        fun bind(color: Int) {
            colorDisplay.setColor(color)

            itemView.isSelected = curColor == color
            itemView.setOnClickListener {
                onColorClickListener?.invoke(color)
            }
        }
    }

    fun setColor(color: Int) {
        curColor = color
        notifyDataSetChanged()
    }
}
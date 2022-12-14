/*
 * Copyright (C) 2018 Softbank Robotics Europe
 * See COPYING for the license
 */

package com.softbankrobotics.dx.peppercodescannersample

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.layout_info_log_view.view.*

/**
 * View holder for the conversation view.
 */
internal class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    /**
     * Bind the text to the view.
     * @param text the text
     */
    fun bind(text: String) {
        itemView.textview.text = text
    }
}

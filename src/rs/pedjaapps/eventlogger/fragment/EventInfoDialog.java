package rs.pedjaapps.eventlogger.fragment;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import rs.pedjaapps.eventlogger.R;
import rs.pedjaapps.eventlogger.constants.EventLevel;
import rs.pedjaapps.eventlogger.constants.EventType;
import rs.pedjaapps.eventlogger.model.Event;

/**
 * Created by pedja on 13.4.14..
 */
public class EventInfoDialog extends DialogFragment
{
    public static final String EXTRA_EVENT = "extra_event";
    Event event;

    public static EventInfoDialog newInstance(Event event)
    {
        EventInfoDialog eventInfoDialog = new EventInfoDialog();
        Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_EVENT, event);
        eventInfoDialog.setArguments(extras);
        return eventInfoDialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        event  = getArguments().getParcelable(EXTRA_EVENT);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.layout_event_info, null);
        TextView tvTimestamp = (TextView)view.findViewById(R.id.tvTimestamp);
        TextView tvEventLevelColor = (TextView)view.findViewById(R.id.tvEventLevelColor);
        TextView tvEventLevelText = (TextView)view.findViewById(R.id.tvEventLevelText);
        TextView tvLongDesc = (TextView)view.findViewById(R.id.tvLongDesc);
        TextView tvEventDetails = (TextView)view.findViewById(R.id.tvEventDetails);
        tvTimestamp.setText(event.getTimestamp().getTime() + "");
        tvEventLevelColor.setBackgroundColor(EventLevel.getLevelForInt(event.getLevel()).color());
        tvLongDesc.setText(Html.fromHtml(event.getLong_desc()));
        tvEventDetails.setCompoundDrawablesWithIntrinsicBounds(EventType.getIconForId(event.getType()), 0, 0, 0);
        tvEventLevelText.setText(EventLevel.getTextForInt(event.getLevel()));
        builder.setView(view);
        return builder.create();
    }
}
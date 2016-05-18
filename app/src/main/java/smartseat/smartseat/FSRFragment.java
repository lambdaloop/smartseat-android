package smartseat.smartseat;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.w3c.dom.Text;


public class FSRFragment extends Fragment {
    private static final String ARG_PAGE_NUMBER = "page_number";
    private DrawingPanel panel;

    public FSRFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fsr_page_layout, container, false);

        LinearLayout fv = (LinearLayout) rootView.findViewById(R.id.top_layout);
//        fv.setBackgroundColor(getResources().getColor(R.color.backgroundColor));
//
//        RelativeLayout card = new RelativeLayout(rootView.getContext());
//        card.setBackgroundColor(Color.WHITE);

        panel = new DrawingPanel(rootView.getContext());
        panel.setBackgroundColor(Color.WHITE);
        fv.addView(panel);

//        TextView text = new TextView(rootView.getContext());
//        text.setText("Hello world!");
//        text.setGravity(View.TEXT_ALIGNMENT_GRAVITY);
//        card.addView(text);

//        fv.addView(card);

        return rootView;
    }

    public void updateSensor(int sensor, double value) {
        if(panel != null) {
            panel.updateSensor(sensor, value);
        }
    }
}

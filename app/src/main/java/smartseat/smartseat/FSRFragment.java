package smartseat.smartseat;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


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
        panel = new DrawingPanel(rootView.getContext());
        fv.addView(panel);

        return rootView;
    }

    public void updateSensor(int sensor, double value) {
        panel.updateSensor(sensor, value);
    }
}

package com.smart.tablet.MarketingPartnerSection;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.smart.tablet.ImagePopupActivity;
import com.smart.tablet.R;

import java.util.List;

public class PartnerGallery extends FragmentActivity {

    GridView gridView;
    List<Integer> ItemsList;
    int selectedItem;
    int[] gallerThumb = {R.drawable.gallery1,
            R.drawable.gallery2,
            R.drawable.gallery3,
            R.drawable.gallery4,
            R.drawable.gallery5,
            R.drawable.gallery6};
    public PartnerGallery() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mp_gallery);
        //final LinearLayout lnr = (LinearLayout) findViewById(R.id.mainBlur);
        gridView = (GridView) findViewById(R.id.list_gallery);
        //GalleryGridAdapter adapter = new GalleryGridAdapter(this, gallerThumb);
        //gridView.setAdapter(adapter);


//              Grid View Listener ID Get
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedItem = (Integer) parent.getItemAtPosition(position);
//              lnr.getBackground().setAlpha(100);
                Intent i = new Intent(getApplicationContext(), ImagePopupActivity.class);
                i.putExtra("IMAGE", selectedItem);
                startActivity(i);
            }
        });

    }
}

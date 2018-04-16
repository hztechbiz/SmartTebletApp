package com.smartapp.hztech.smarttebletapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.smartapp.hztech.smarttebletapp.R;
import com.smartapp.hztech.smarttebletapp.entities.Category;

import java.util.List;

public class CategoryGridAdapter extends BaseAdapter {

    private View.OnClickListener itemClickListener;
    private List<Category> categories;
    private Context context;
    private LayoutInflater inflater;

    public CategoryGridAdapter(Context context, List<Category> categories, View.OnClickListener itemClickListener) {
        this.context = context;
        this.categories = categories;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Category getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View gridView = convertView;

        if (convertView == null) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            gridView = inflater.inflate(R.layout.category_item, null);
        }

        Category category = getItem(position);

        LinearLayout box_categories = gridView.findViewById(R.id.bx_category);

        ((TextView) gridView.findViewById(R.id.txt_title)).setText(category.getName());
        ((TextView) gridView.findViewById(R.id.txt_description)).setText(category.getDescription());

        box_categories.setTag(category.getId());
        box_categories.setTag(R.string.tag_has_children, (category.getChildren_count() > 0));

        box_categories.setOnClickListener(itemClickListener);

        return gridView;
    }
}
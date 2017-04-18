package job.com.searchnearbyplaces;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import job.com.searchnearbyplaces.model.Result;

public class ListActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private MyApplication myApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        myApplication = MyApplication.getApp();

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        DataAdapter dataAdapter = new DataAdapter();
        mRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(ListActivity.this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(dataAdapter);

    }

    public class DataAdapter extends RecyclerView.Adapter<DataAdapter.MyViewHolder>{

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item,null);
            MyViewHolder myViewHolder = new MyViewHolder(view);
            return myViewHolder;
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            Result result = myApplication.resultList.get(position);
            String place_name = result.getName();
            String address = result.getFormattedAddress();

            holder.txtName.setText(place_name);
            holder.txtAddress.setText(address);
            holder.setClickListener(new OnItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    if(!isLongClick){
                        Intent intent = new Intent(ListActivity.this, DetailActivity.class);
                        intent.putExtra("position",""+position);
                        startActivity(intent);
                    }
                }
            });

        }

        @Override
        public int getItemCount() {
            return myApplication.resultList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener, View.OnClickListener {

            private TextView txtName, txtAddress;
            private OnItemClickListener itemClickListener;

            public MyViewHolder(View itemView) {
                super(itemView);
                txtName = (TextView) itemView.findViewById(R.id.list_name);
                txtAddress = (TextView) itemView.findViewById(R.id.list_address);
                itemView.setOnClickListener(this);
                itemView.setOnLongClickListener(this);
            }

            public void setClickListener(OnItemClickListener itemClickListener) {
                this.itemClickListener = itemClickListener;
            }

            @Override
            public void onClick(View v) {
                itemClickListener.onClick(v, getAdapterPosition(), false);
            }

            @Override
            public boolean onLongClick(View v) {
                itemClickListener.onClick(v, getAdapterPosition(), true);
                return true;
            }
        }
    }
}

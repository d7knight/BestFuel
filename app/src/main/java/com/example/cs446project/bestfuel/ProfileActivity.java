package com.example.cs446project.bestfuel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.example.cs446project.bestfuel.helper.SQLiteHandler;
import com.example.cs446project.bestfuel.helper.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ProfileActivity extends Activity {

    private TextView txtInfo;
    private SQLiteHandler db;
    private SessionManager session;
    private ArrayList<String> carYears = new ArrayList<String>();
    private View inflated;
    private Spinner yearSpin;
    private int setYear;
    private Spinner makeSpin;
    private String setMake;
    private Spinner modelSpin;
    private String setModel;
    private Spinner trimSpin;
    private String setTrim;
    private String setTrimId;
    private ArrayList<String> trimId = new ArrayList<String>();
    private TextView carText;
    private String fullCarString;
    private ProgressDialog pDialog;
    private Dialog addDialog;
    ListView list;
    CarAdapter adapter;
    ArrayList<Car> arraylist=new ArrayList<Car>();

    CQInterface cq;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.activity_profile);

        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
        }

        // Pass results to ListViewAdapter Class
        adapter = new CarAdapter(this, arraylist);

        // Binds the Adapter to the ListView
        list = (ListView) findViewById(R.id.listview);
        list.setAdapter(adapter);

        adapter.notifyDataSetChanged();


        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        // Fetching user details from sqlite
        HashMap<String, String> user = db.getUserDetails();

        //Profile Data ========================================
        final String name = user.get("name");
        String email = user.get("email");

        TextView nameTxt = (TextView) findViewById(R.id.profileName);
        nameTxt.setText(name);

        TextView emailTxt = (TextView) findViewById(R.id.profileEmail);
        emailTxt.setText(email);

        //Profile end ========================================

        //Add car button
        FrameLayout addFrame = (FrameLayout) findViewById(R.id.addFrame);
        addFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("CarFrame", "Clicked on add a car");
                addCarDialogue(inflated);

                cq.getYears();
            }
        });


        //Carquery Initialization stuff
        WebView webview = (WebView)findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        cq = new CQInterface(this, webview);
        webview.addJavascriptInterface(cq, "Android");

        webview.loadUrl("file:///android_asset/carquery.html");


        addDialog = new Dialog(this);


        inflated = getLayoutInflater().inflate(R.layout.vehicle_layout, null);
        yearSpin = (Spinner) inflated.findViewById(R.id.year);
        makeSpin = (Spinner) inflated.findViewById(R.id.make);
        modelSpin = (Spinner) inflated.findViewById(R.id.model);
        trimSpin = (Spinner) inflated.findViewById(R.id.trim);
        carText = (TextView) inflated.findViewById(R.id.carData);
        Button yesBtn = (Button) inflated.findViewById(R.id.carDialogYes);
        Button noBtn = (Button) inflated.findViewById(R.id.carDialogNo);


        populateCars(user.get("name"), db);

        yesBtn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                //check if all fields are filled out
                //if(trimSpin.getAdapter().getCount() >0) {}
                if(carText.getText() != ""){
                    //TODO actually make use of the tons of car data!
                    //ideas include: show base data on profile. Click and opens up to lots of great stuff.
                    //give option of either Liter per km or Mpg
                    String cap = "";
                    String hwy ="";
                    String city ="";
                    try {
                        JSONArray jArray = new JSONArray(fullCarString);
                        JSONObject jObj = jArray.getJSONObject(0);
                        addCarToDB(fullCarString, name);
                        cap = jObj.getString("model_fuel_cap_l") +"L Tank";
                        hwy = jObj.getString("model_lkm_hwy")+" L/Km Highway";
                        city = jObj.getString("model_lkm_city")+" L/Km City";
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    adapter.add(new Car(setMake, setModel, Integer.toString(setYear), cap, hwy, city));
                    adapter.notifyDataSetChanged();
                    addDialog.dismiss();
                }
            }
        });

        noBtn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View v) {
                addDialog.dismiss();
            }
        });


    }

    public void populateCars(String name, SQLiteHandler db) {
        ArrayList<HashMap<String, String>> retList = db.getCars(name);
        for (int i=0; i<retList.size(); i++) {
            adapter.add(new Car(retList.get(i).get("make"), retList.get(i).get("model"), retList.get(i).get("year"), "" +retList.get(i).get("fuel_capacity_l")+" L Tank" , ""+retList.get(i).get("hwy_lkm")+" L/Km City", ""+retList.get(i).get("city_lkm")+" L/Km Highway"));
        }
    }

    public void addCarToDB(String carString, String name) {
        try{
            JSONArray jArr = new JSONArray(carString);
            JSONObject jObj = jArr.getJSONObject(0);
            boolean isdefault=true;
            if(arraylist.size()>0) {
                isdefault =false;
            }
            db.addCar(name, isdefault, Integer.toString(setYear), setMake,setModel,jObj.getString("model_id"),jObj.getString("model_body"),
                    jObj.getString("model_engine_position"), jObj.getString("model_engine_cc"),jObj.getString("model_engine_cyl"),
                    jObj.getString("model_engine_type"), jObj.getString("model_engine_valves_per_cyl"), jObj.getString("model_engine_power_rpm"), jObj.getString("model_engine_fuel"),
                   jObj.getString("model_top_speed_kph"),jObj.getString("model_0_to_100_kph"),jObj.getString("model_drive"), jObj.getString("model_transmission_type"),
                   jObj.getString("model_seats"), jObj.getString("model_doors"), jObj.getString("model_weight_kg"), jObj.getString("model_length_mm"),
                   jObj.getString("model_height_mm"),jObj.getString("model_width_mm"), jObj.getString("model_wheelbase_mm"), jObj.getString("model_lkm_hwy"),
                   jObj.getString("model_lkm_mixed"), jObj.getString("model_lkm_city"), jObj.getString("model_fuel_cap_l"), jObj.getString("model_sold_in_us"),
                   jObj.getString("model_engine_power_hp"), jObj.getString("model_top_speed_mph"), jObj.getString("model_weight_lbs"), jObj.getString("model_length_in"),
                   jObj.getString("model_width_in"), jObj.getString("model_height_in"), jObj.getString("model_mpg_hwy"), jObj.getString("model_mpg_city"),
                    jObj.getString("model_mpg_mixed"), jObj.getString("model_fuel_cap_g"), jObj.getString("make_country"));

            Log.d("ADDCARTODB", "returned");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


   public class Car{
       String make,model,year,capacity,hwy,city;
       public Car(String make, String model, String year, String capacity, String hwy, String city){
           this.make=make;
           this.model=model;
           this.year=year;
           this.capacity=capacity;
           this.hwy=hwy;
           this.city=city;

       }
   }


    public class CarAdapter extends ArrayAdapter<Car> {

        // Declare Variables
        Context mContext;
        LayoutInflater inflater;
        private List<Car> carlist = null;
        private ArrayList<Car> arraylist;
        public CarAdapter(Context context, ArrayList<Car> cars) {
            super(context, 0, cars);
            mContext = context;
            this.carlist = cars;
            inflater = LayoutInflater.from(mContext);
            this.arraylist = new ArrayList<Car>();
            this.arraylist.addAll(carlist);
        }


        public class ViewHolder {
            View view;
            TextView make,model,year,capacity,hwy,city;

        }


        public View getView(final int position, View view, ViewGroup parent) {
            final ViewHolder holder;
            if (view == null) {
                holder = new ViewHolder();

                view = inflater.inflate(R.layout.car_template,parent, false);

                holder.view=view;
                holder.make= (TextView)view.findViewWithTag("Make");
                holder.model= (TextView)view.findViewWithTag("Model");
                holder.year= (TextView)view.findViewWithTag("Year");
                holder.capacity= (TextView)view.findViewWithTag("Capacity");
                holder.hwy= (TextView)view.findViewWithTag("Hwy");
                holder.city= (TextView)view.findViewWithTag("City");
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            Car c=carlist.get(position);
            holder.make.setText(c.make);
            holder.model.setText(c.model);
            holder.year.setText(c.year);
            holder.capacity.setText(c.capacity);
            holder.hwy.setText(c.hwy);
            holder.city.setText(c.city);



            view.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View arg0) {
                    // Send single item click data to SingleItemView Class
                    final View customView = inflater.inflate(R.layout.manage_car_dialog, null);
                    AlertDialog dialog = new AlertDialog.Builder(ProfileActivity.this)
                            .setTitle("Car Settings")
                            .setView(customView)
                            .setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {

                                }
                            }).create();
                    dialog.show();
                    return true;

                }
            });


            return view;
        }



    }


    private void logoutUser() {
        session.setLogin(false);

        db.deleteUsers();

        // Launching the login activity
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    public void addCarDialogue(View inflated){

         //addDialog = new Dialog(this);
        addDialog.setContentView(inflated);
        addDialog.setTitle("Add A Vehicle");
        yearSpin.setAdapter(null);
        makeSpin.setAdapter(null);
        modelSpin.setAdapter(null);
        trimSpin.setAdapter(null);
        carText.setText("Enter Information To Search for your car");

        addDialog.show();
    }

    private void adjustYearSpinner(ArrayList<String> years){
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ProfileActivity.this, android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpin.setAdapter(adapter);
        yearSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String curYear = yearSpin.getSelectedItem().toString();
                if (curYear != "Years") {
                    setYear = Integer.parseInt(curYear);
                    cq.getMakes(setYear);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }



    //Loading dialog stuff
    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }


    public class CQInterface {
        Context context;
        WebView webview;

        CQInterface(Context c, WebView wb) {
            context = c;
            webview = wb;


        }

        @JavascriptInterface
        public void testCall(View v){
            Log.d("CQ2", "running test call");
            Object retObj=null;
            getMakes(2009);
        }


        @JavascriptInterface
        public void initd(String str) {
            Log.d("CQ", "done initializing " + str);
        }

        //returns the range of all possible years for vehicles
        @JavascriptInterface
        public void getYears(){
            Log.d("CarQuery", "Calling getYears");
            pDialog.setMessage("Fetching Vehicle Years ...");
            showDialog();
            webview.loadUrl("javascript:getYears()");
        }

        //RETURNS: JSON String {"min_year":"#", "max_year":"#"}
        @JavascriptInterface
        public void getYearsRet(String retObj) {

            Log.d("CQ", "the actual retobj is " + retObj);

            final ArrayList<String> years = new ArrayList<String>();
            JSONObject jObj = null;
            int start=0;
            int end=0;
            try {
                jObj = new JSONObject(retObj);
                start = Integer.parseInt(jObj.getString("min_year"));
                end = Integer.parseInt(jObj.getString("max_year"));
            } catch (JSONException e) {
                Log.d("CQ", "getYearsRet parse error");
                e.printStackTrace();
            }
            years.add("Years");
            for(int i=end; i>= start; i--) {
                years.add(Integer.toString(i));
            }
            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  adjustYearSpinner(years);
                              }
                          });

            hideDialog();
        }

        //returns all the possible makes in a specified year
        @JavascriptInterface
        public void getMakes(int year){
            pDialog.setMessage("Fetching Vehicle Makes ...");
            showDialog();
            webview.loadUrl("javascript:getMakes(" + year + ")");
        }

        //RETURNS: JSON String [{"make_id":"ac","make_display":"AC","make_is_common":"0","make_country":"UK"},
        //{"make_id":"acura","make_display":"Acura","make_is_common":"1","make_country":"USA"}...]
        @JavascriptInterface
        public void getMakesRet(String retObj) {
            Log.d("CQ", "the actual retobj for makes is " + retObj);
            final ArrayList<String> makes = new ArrayList<String>();
            int len=0;
            JSONArray jArray=null;
            try {
                jArray = new JSONArray(retObj);
                len= jArray.length();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONObject jObj=null;
            makes.add("Makes");
            for(int i=0; i< len; i++) {
                try {
                    jObj= jArray.getJSONObject(i);
                    makes.add(jObj.getString("make_display"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(ProfileActivity.this, android.R.layout.simple_spinner_item, makes);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    makeSpin.setAdapter(adapter);
                    makeSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                            String curMake = makeSpin.getSelectedItem().toString();
                            if (curMake != "Makes") {
                                    setMake = curMake;
                                    cq.getModels(curMake, setYear);
                                Log.d("ProfileCQ", "calling from makes spinner");
                                }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }
            });

            hideDialog();
        }

        //gets all the models from a specified make in a specified year
        //make example = "ford"
        @JavascriptInterface
        public void getModels(String make, int year) {
            Log.d("CQ", "Grabbing models of "+make +" in " + year);
            pDialog.setMessage("Fetching Vehicle Models ...");
            showDialog();
            webview.loadUrl("javascript:getModels(\"" + make + "\",\"" + year + "\")");
        }

        //RETURNS: JSON String [{"model_name":"Escape","model_make_id":"ford"}...]
        @JavascriptInterface
        public void getModelsRet(String retObj) {
            Log.d("CQ", "the actual retobj for models is " + retObj);
            final ArrayList<String> models = new ArrayList<String>();
            int len=0;
            JSONArray jArray=null;
            try {
                jArray = new JSONArray(retObj);
                len= jArray.length();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONObject jObj=null;
            models.add("Models");
            for(int i=0; i< len; i++) {
                try {
                    jObj= jArray.getJSONObject(i);
                    models.add(jObj.getString("model_name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(ProfileActivity.this, android.R.layout.simple_spinner_item, models);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    modelSpin.setAdapter(adapter);
                    modelSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                            String curModel = modelSpin.getSelectedItem().toString();
                            Log.d("Profile", "current select model item is "+ curModel);
                            if (curModel != "Models") {
                                    setModel=curModel;
                                    cq.getData(setMake, curModel, setYear, 0);
                                }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }
            });

            hideDialog();

        }

        //gets all the details you need including vehicle id. Necessary step as there can be
        //many versions of the same/similar vehicle
        //PARAMETERS: fullDetails is 1 for all data or 0 for sparse data, test both to see what you need
        @JavascriptInterface
        public void getData(String make, String model, int year, int fullDetails) {
            pDialog.setMessage("Fetching Vehicle Data ...");
            showDialog();
            webview.loadUrl("javascript:getData(\"" + make + "\",\"" + year + "\",\"" + model + "\",\"" + fullDetails + "\")");
        }

        //returns a ton of data... print out to test it
        @JavascriptInterface
        public void getDataRet(String retObj) {
            Log.d("CQ", "the actual retobj for data is " + retObj);
            final ArrayList<String> trim = new ArrayList<String>();
            int len=0;
            trimId.clear();
            JSONArray jArray=null;
            try {
                jArray = new JSONArray(retObj);
                len= jArray.length();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONObject jObj=null;
            trim.add("Vehicle");
            for(int i=0; i< len; i++) {
                try {
                    jObj= jArray.getJSONObject(i);
                    trim.add(jObj.getString("model_trim"));
                    trimId.add(jObj.getString("model_id"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(ProfileActivity.this, android.R.layout.simple_spinner_item, trim);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    trimSpin.setAdapter(adapter);
                    trimSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                            String curTrim = trimSpin.getSelectedItem().toString();
                            if (curTrim != "Vehicle") {
                                setTrim=curTrim;
                                setTrimId=trimId.get(position-1);
                                cq.getVehicle(Integer.parseInt(setTrimId));
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                }
            });

            hideDialog();
        }

        //you need to grab vehicleId from getData and run it through here to get the exact car
        @JavascriptInterface
        public void getVehicle(int vehicleID) {
            Log.d("CQ", "getting vehicle with id " + vehicleID);
            pDialog.setMessage("Fetching Vehicle ...");
            showDialog();
            webview.loadUrl("javascript:getVehicle(\"" + vehicleID + "\")");
        }

        //returns all available details about the car. print out to test
        @JavascriptInterface
        public void getVehicleRet(String retObj) {
            Log.d("CQ", "the actual retobj for vehicle is " + retObj);
            fullCarString=retObj;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    carText.setText("Is " + setTrim + " your car?");
                }
            });
            hideDialog();
        }


    }

}

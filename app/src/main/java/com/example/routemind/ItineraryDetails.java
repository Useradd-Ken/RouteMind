package com.example.routemind;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public class ItineraryDetails extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;
    private RatingBar ratingBarSelector;
    private AutoCompleteTextView spinnerCategory;
    private TextView tvLabel, tvSummaryFood, tvSummaryAccommodation, tvSummaryPlaces;
    private EditText etComment;
    private ImageView ivSelectedImage;
    private View placeholderContainer;
    private SwitchMaterial switchPostPublic;
    private Uri imageUri;
    private String destination = "Destination";
    
    private float foodRating = 0, accommodationRating = 0, placesRating = 0;
    private String[] categories = {"Food", "Accommodation", "Places"};

    private static final String IMGBB_API_KEY = "58c804fdeffd6d02885edda7a5d6de13";

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivSelectedImage.setImageURI(imageUri);
                    ivSelectedImage.setVisibility(View.VISIBLE);
                    placeholderContainer.setVisibility(View.GONE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_itinerary_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        initFirebase();
        initViews();
        setupRatingLogic();
    }

    private void initFirebase() {
        try {
            db = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();
            
            if ((MainActivity.sessionUsername == null || MainActivity.sessionUsername.isEmpty()) && mAuth.getCurrentUser() != null) {
                MainActivity.sessionEmail = mAuth.getCurrentUser().getEmail();
                MainActivity.sessionUsername = dbHelper.getUsernameByEmail(MainActivity.sessionEmail);
                if (MainActivity.sessionUsername == null || MainActivity.sessionUsername.isEmpty()) {
                    MainActivity.sessionUsername = dbHelper.getName(MainActivity.sessionEmail);
                }
                if (MainActivity.sessionUsername == null || MainActivity.sessionUsername.isEmpty()) {
                    MainActivity.sessionUsername = MainActivity.sessionEmail.split("@")[0];
                }
            }
        } catch (Exception e) {
            Log.e("ItineraryDetails", "Firebase initialization error", e);
        }
    }

    private void initViews() {
        spinnerCategory = findViewById(R.id.spinner_category);
        ratingBarSelector = findViewById(R.id.rating_bar_selector);
        tvLabel = findViewById(R.id.tv_current_rating_label);
        tvSummaryFood = findViewById(R.id.tv_summary_food);
        tvSummaryAccommodation = findViewById(R.id.tv_summary_accommodation);
        tvSummaryPlaces = findViewById(R.id.tv_summary_places);
        
        etComment = findViewById(R.id.et_feedback_comment);
        ivSelectedImage = findViewById(R.id.iv_selected_image);
        placeholderContainer = findViewById(R.id.placeholder_container);
        switchPostPublic = findViewById(R.id.switch_post_public);
        TextView tvDest = findViewById(R.id.tv_detail_destination);
        
        Itinerary itinerary = (Itinerary) getIntent().getSerializableExtra("itinerary");
        if (itinerary != null) {
            destination = itinerary.getDestination();
            if (tvDest != null) tvDest.setText(destination);
        }

        findViewById(R.id.btn_select_image).setOnClickListener(v -> {
            imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
        });

        ivSelectedImage.setOnClickListener(v -> {
            imageUri = null;
            ivSelectedImage.setVisibility(View.GONE);
            placeholderContainer.setVisibility(View.VISIBLE);
            Snackbar.make(v, "Image removed", Snackbar.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_submit_rating).setOnClickListener(this::handleFeedbackSubmission);
    }

    private void setupRatingLogic() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setText(categories[0], false);
        
        spinnerCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selected = categories[position];
            tvLabel.setText("Rating for " + selected);
            
            if (selected.equals("Food")) ratingBarSelector.setRating(foodRating);
            else if (selected.equals("Accommodation")) ratingBarSelector.setRating(accommodationRating);
            else if (selected.equals("Places")) ratingBarSelector.setRating(placesRating);
        });

        ratingBarSelector.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (!fromUser) return;
            String current = spinnerCategory.getText().toString();
            if (current.equals("Food")) {
                foodRating = rating;
                tvSummaryFood.setText("Food: " + (int)rating + "★");
            } else if (current.equals("Accommodation")) {
                accommodationRating = rating;
                tvSummaryAccommodation.setText("Stay: " + (int)rating + "★");
            } else if (current.equals("Places")) {
                placesRating = rating;
                tvSummaryPlaces.setText("Places: " + (int)rating + "★");
            }
        });
    }

    private void handleFeedbackSubmission(View view) {
        int ratedCount = 0;
        if (foodRating > 0) ratedCount++;
        if (accommodationRating > 0) ratedCount++;
        if (placesRating > 0) ratedCount++;

        if (ratedCount < 2) {
            Snackbar.make(view, "Please rate at least 2 categories.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String comment = etComment.getText().toString().trim();
        boolean isPublic = switchPostPublic.isChecked();

        if (db == null || mAuth == null) {
            Snackbar.make(view, "Connection error. Please try again.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadToImgBB(view, foodRating, accommodationRating, placesRating, comment, isPublic);
        } else {
            saveFeedbackToFirestore(view, foodRating, accommodationRating, placesRating, comment, "", isPublic);
        }
    }

    private void uploadToImgBB(View view, float food, float accommodation, float places, String comment, boolean isPublic) {
        final Snackbar snackbar = Snackbar.make(view, "Uploading photo...", Snackbar.LENGTH_INDEFINITE);
        snackbar.show();

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            String base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.imgbb.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ImgBBService service = retrofit.create(ImgBBService.class);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("image", base64Image);

            service.uploadImage(IMGBB_API_KEY, filePart).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    snackbar.dismiss();
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String json = response.body().string();
                            JSONObject jsonObject = new JSONObject(json);
                            String url = jsonObject.getJSONObject("data").getString("url");
                            saveFeedbackToFirestore(view, food, accommodation, places, comment, url, isPublic);
                        } catch (Exception e) {
                            saveFeedbackToFirestore(view, food, accommodation, places, comment, "", isPublic);
                        }
                    } else {
                        saveFeedbackToFirestore(view, food, accommodation, places, comment, "", isPublic);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    snackbar.dismiss();
                    saveFeedbackToFirestore(view, food, accommodation, places, comment, "", isPublic);
                }
            });

        } catch (IOException e) {
            snackbar.dismiss();
            saveFeedbackToFirestore(view, food, accommodation, places, comment, "", isPublic);
        }
    }

    private void saveFeedbackToFirestore(View view, float food, float accommodation, float places, String comment, String imageUrl, boolean isPublic) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        String userName = MainActivity.sessionUsername != null ? MainActivity.sessionUsername : "Explorer";
        String userPhoto = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
        
        String id = db.collection("feedback").document().getId();
        Feedback feedback = new Feedback(id, userId, userName, userPhoto, destination, comment, imageUrl, food, accommodation, places, System.currentTimeMillis());
        feedback.setPublic(isPublic);
        
        Itinerary itinerary = (Itinerary) getIntent().getSerializableExtra("itinerary");
        if (itinerary != null) {
            feedback.setItineraryId(itinerary.getId());
            feedback.setItineraryTitle(itinerary.getTitle());
        }
        
        db.collection("feedback").document(id).set(feedback).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                if (isPublic) {
                    db.collection("reviews").document(id).set(feedback);
                    if (itinerary != null && itinerary.getId() != null) {
                         db.collection("booked_itineraries").document(itinerary.getId())
                                 .update("usageCount", FieldValue.increment(1));
                    }
                }
                Snackbar.make(view, "Experience shared!", Snackbar.LENGTH_SHORT).show();
                view.postDelayed(this::finish, 1500);
            }
        });
    }

    public interface ImgBBService {
        @Multipart
        @POST("1/upload")
        Call<ResponseBody> uploadImage(
                @Query("key") String apiKey,
                @Part MultipartBody.Part image
        );
    }
}

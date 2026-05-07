package com.example.routemind;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;

public class ItineraryDetails extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseStorage mStorage;
    private RatingBar ratingBar;
    private EditText etComment;
    private ImageView ivSelectedImage;
    private View placeholderContainer;
    private SwitchMaterial switchPostPublic;
    private Uri imageUri;
    private String destination = "Destination";

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

        initFirebase();
        initViews();
    }

    private void initFirebase() {
        try {
            mDatabase = FirebaseDatabase.getInstance().getReference("feedbacks");
            mStorage = FirebaseStorage.getInstance();
        } catch (Exception e) {
            Log.e("ItineraryDetails", "Firebase error", e);
        }
    }

    private void initViews() {
        ratingBar = findViewById(R.id.rating_bar);
        etComment = findViewById(R.id.et_feedback_comment);
        ivSelectedImage = findViewById(R.id.iv_selected_image);
        placeholderContainer = findViewById(R.id.placeholder_container);
        switchPostPublic = findViewById(R.id.switch_post_public);
        TextView tvDest = findViewById(R.id.tv_detail_destination);
        
        if (tvDest != null) {
            String fullText = tvDest.getText().toString();
            if (fullText.contains(": ")) destination = fullText.split(": ")[1];
        }

        findViewById(R.id.btn_select_image).setOnClickListener(v -> {
            imagePickerLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_submit_rating).setOnClickListener(this::handleFeedbackSubmission);
    }

    private void handleFeedbackSubmission(View view) {
        float rating = ratingBar.getRating();
        String comment = etComment.getText().toString().trim();

        if (rating == 0) {
            Snackbar.make(view, "Please rate your experience.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (!switchPostPublic.isChecked()) {
            Snackbar.make(view, "Feedback saved privately.", Snackbar.LENGTH_SHORT).show();
            view.postDelayed(this::finish, 1500);
            return;
        }

        if (comment.isEmpty() || imageUri == null) {
            Snackbar.make(view, "Add a comment and photo to post publicly.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (mDatabase != null && mStorage != null) uploadImageAndPost(view, rating, comment);
        else {
            Snackbar.make(view, "Offline: Saved locally.", Snackbar.LENGTH_SHORT).show();
            view.postDelayed(this::finish, 1500);
        }
    }

    private void uploadImageAndPost(View view, float rating, String comment) {
        Snackbar.make(view, "Sharing with community...", Snackbar.LENGTH_INDEFINITE).show();
        StorageReference ref = mStorage.getReference().child("feedback_images/" + UUID.randomUUID().toString());

        ref.putFile(imageUri).addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            String id = mDatabase.push().getKey();
            Feedback feedback = new Feedback(id, "You", "", destination, comment, uri.toString(), rating, System.currentTimeMillis());
            mDatabase.child(id).setValue(feedback).addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    Snackbar.make(view, "Success!", Snackbar.LENGTH_SHORT).show();
                    view.postDelayed(this::finish, 1000);
                }
            });
        })).addOnFailureListener(e -> Snackbar.make(view, "Upload failed.", Snackbar.LENGTH_LONG).show());
    }
}

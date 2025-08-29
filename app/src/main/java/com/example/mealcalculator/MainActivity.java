package com.example.mealcalculator;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ScrollView scrollView;
    private TextView dateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scrollView = findViewById(R.id.root_layout);
        dateText = findViewById(R.id.date_text);

        Button calculateButton = findViewById(R.id.calculate_button);
        Button shareButton = findViewById(R.id.share_button);
        shareButton.setEnabled(false);

        TextView totalBazarText = findViewById(R.id.total_bazar);
        TextView totalUtilityText = findViewById(R.id.total_utility);
        TextView totalMealsText = findViewById(R.id.total_meals);
        TextView mealRateText = findViewById(R.id.meal_rate);

        calculateButton.setOnClickListener(v -> {
            try {
                float totalBazar = 0, totalMeals = 0, totalUtility = 0;
                float[] bazarExpenses = {
                        parseOrZero("bazar_sayeed"),
                        parseOrZero("bazar_saklain"),
                        parseOrZero("bazar_shishir"),
                        parseOrZero("bazar_farhan")
                };
                float[] meals = {
                        parseOrZero("meals_sayeed"),
                        parseOrZero("meals_saklain"),
                        parseOrZero("meals_shishir"),
                        parseOrZero("meals_farhan")
                };
                float[] utilityExpenses = {
                        parseOrZero("utility_sayeed"),
                        parseOrZero("utility_saklain"),
                        parseOrZero("utility_shishir"),
                        parseOrZero("utility_farhan")
                };

                for (int i = 0; i < 4; i++) {
                    totalBazar += bazarExpenses[i];
                    totalMeals += meals[i];
                    totalUtility += utilityExpenses[i];
                }

                if (totalMeals == 0) {
                    Toast.makeText(this, "Total meals cannot be zero.", Toast.LENGTH_SHORT).show();
                    return;
                }

                float mealRate = totalBazar / totalMeals;

                totalBazarText.setText(String.format("Total Bazar Expense: %.2f Taka", totalBazar));
                totalUtilityText.setText(String.format("Total Utility Cost: %.2f Taka", totalUtility));
                totalMealsText.setText(String.format("Total Meals: %d", (int) totalMeals));
                mealRateText.setText(String.format("Meal Rate: %.2f Taka/Meal", mealRate));

                // ✅ Show generated date now
                String currentDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
                dateText.setText("Generated on: " + currentDate);
                dateText.setVisibility(View.VISIBLE);

                // Member summary
                String[] members = {"Sayeed", "Saklain", "Shishir", "Farhan"};
                StringBuilder resultBuilder = new StringBuilder();

                float utilityPerPerson = totalUtility / 4;
                for (int i = 0; i < 4; i++) {
                    float mealCost = meals[i] * mealRate;
                    float totalCost = mealCost + utilityPerPerson;
                    float balance = bazarExpenses[i] + utilityExpenses[i] - totalCost;
                    float totalPaid = bazarExpenses[i] + utilityExpenses[i];

                    String balanceResult = String.format(Locale.getDefault(),
                            "%s: Paid = %.2f, Meal Cost = %.2f, Utility Cost: = %.2f, Balance = %.2f → %s\n",
                            members[i], totalPaid, mealCost, utilityPerPerson, Math.abs(balance),
                            (balance > 0) ? "Will Get\n" : (balance < 0) ? "Will Pay\n" : "Settled\n");

                    resultBuilder.append(balanceResult);
                }

                TextView balanceResultText = findViewById(R.id.balance_result);
                balanceResultText.setText(resultBuilder.toString());
                balanceResultText.setVisibility(View.VISIBLE);

                shareButton.setEnabled(true);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Invalid inputs!", Toast.LENGTH_SHORT).show();
            }
        });

        shareButton.setOnClickListener(v -> captureAndShareAsPdf());
    }

    private float parseOrZero(String id) {
        EditText input = findViewById(getResources().getIdentifier(id, "id", getPackageName()));
        try {
            return input.getText().toString().trim().isEmpty() ? 0 : Float.parseFloat(input.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void captureAndShareAsPdf() {
        try {
            // Show current date in layout
            String currentDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
            dateText.setText("Generated on: " + currentDate);
            dateText.setVisibility(View.VISIBLE);

            View content = scrollView.getChildAt(0);
            content.measure(
                    View.MeasureSpec.makeMeasureSpec(content.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            content.layout(0, 0, content.getMeasuredWidth(), content.getMeasuredHeight());

            int pageWidth = content.getMeasuredWidth();
            int pageHeight = content.getMeasuredHeight();

            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);
            content.draw(canvas);
            pdfDocument.finishPage(page);

            File pdfDir = new File(getExternalFilesDir(null), "MealCalculator");
            if (!pdfDir.exists()) pdfDir.mkdirs();

            String fileName = "meal_result_" + System.currentTimeMillis() + ".pdf";
            File pdfFile = new File(pdfDir, fileName);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            fos.close();
            pdfDocument.close();

            dateText.setVisibility(View.GONE); // Hide date after drawing

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF via"));

        } catch (IOException e) {
            Toast.makeText(this, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

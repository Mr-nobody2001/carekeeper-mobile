package com.example.carekeeper.ui.emergency;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carekeeper.R;

import java.util.Arrays;
import java.util.List;

public class EmergencyContactsFragment extends Fragment {

    private String numeroPendendeLigacao = null;

    private final List<EmergencyContact> contacts = Arrays.asList(
            new EmergencyContact("Polícia", "190", R.drawable.ic_police_car),
            new EmergencyContact("SAMU", "192", R.drawable.ic_ambulance),
            new EmergencyContact("Bombeiros", "193", R.drawable.ic_fire_truck)
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_emergency_contacts, container, false);

        RecyclerView recyclerView = root.findViewById(R.id.recyclerViewEmergencyContacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new EmergencyContactsAdapter());

        return root;
    }

    private class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_emergency_contact, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EmergencyContact contact = contacts.get(position);
            holder.name.setText(contact.name);
            holder.icon.setImageResource(contact.iconRes);

            // Ação ao clicar no item inteiro
            holder.itemView.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED) {
                    numeroPendendeLigacao = contact.phone;
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 100);
                } else {
                    ligarNumero(contact.phone);
                }
            });
        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView icon;

            ViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.textViewEmergencyName);
                icon = itemView.findViewById(R.id.imageViewEmergencyIcon);
            }
        }
    }

    private void ligarNumero(String numero) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + numero));
            startActivity(intent);
        } catch (SecurityException e) {
            Toast.makeText(getContext(), "Permissão negada para ligar", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (numeroPendendeLigacao != null) {
                    ligarNumero(numeroPendendeLigacao);
                    numeroPendendeLigacao = null;
                }
            } else {
                Toast.makeText(getContext(), "Permissão para ligar negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static class EmergencyContact {
        final String name;
        final String phone;
        final int iconRes;

        EmergencyContact(String name, String phone, int iconRes) {
            this.name = name;
            this.phone = phone;
            this.iconRes = iconRes;
        }
    }
}

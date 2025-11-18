package com.example.carekeeper.ui.emergency;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carekeeper.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsFragment extends Fragment {

    private static final String PREFS_NAME = "emergency_contacts_prefs";
    private static final String KEY_CUSTOM_CONTACTS = "custom_contacts";
    private static final int MAX_CUSTOM_CONTACTS = 3;

    private String numeroPendendeLigacao = null;
    private EmergencyContactsAdapter adapter;

    private final List<EmergencyContact> contacts = new ArrayList<>();
    private final List<EmergencyContact> customContacts = new ArrayList<>();

    private Uri imagemSelecionada = null;
    private ShapeableImageView imagePreview;
    private ActivityResultLauncher<Intent> seletorImagemLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_emergency_contacts, container, false);

        // Contatos fixos
        contacts.add(new EmergencyContact("Polícia", "190", null, R.drawable.ic_police_car));
        contacts.add(new EmergencyContact("SAMU", "192", null, R.drawable.ic_ambulance));
        contacts.add(new EmergencyContact("Bombeiros", "193", null, R.drawable.ic_fire_truck));

        carregarContatosPersonalizados();

        RecyclerView recyclerView = root.findViewById(R.id.recyclerViewEmergencyContacts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EmergencyContactsAdapter();
        recyclerView.setAdapter(adapter);

        // Swipe para deletar
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // não vamos mover itens
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position >= contacts.size()) { // apenas contatos personalizados
                    EmergencyContact removed = customContacts.remove(position - contacts.size());
                    salvarContatosPersonalizados();
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Contato removido: " + removed.name, Toast.LENGTH_SHORT).show();
                } else {
                    adapter.notifyItemChanged(position); // não permite remover contatos fixos
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Launcher para selecionar imagens da galeria
        seletorImagemLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        imagemSelecionada = result.getData().getData();
                        if (imagemSelecionada != null && imagePreview != null) {
                            imagePreview.setImageURI(imagemSelecionada);

                            final int takeFlags = result.getData().getFlags()
                                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            try {
                                requireContext().getContentResolver()
                                        .takePersistableUriPermission(imagemSelecionada, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );

        FloatingActionButton fabAdd = root.findViewById(R.id.fabAddContact);
        fabAdd.setOnClickListener(v -> mostrarDialogoAdicionarContato());

        return root;
    }

    private void mostrarDialogoAdicionarContato() {
        if (customContacts.size() >= MAX_CUSTOM_CONTACTS) {
            Toast.makeText(getContext(), "Você só pode adicionar até 3 contatos personalizados.", Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mostrarDialogoContato(null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void mostrarDialogoContato(EmergencyContact contatoExistente) {
        imagemSelecionada = contatoExistente != null && contatoExistente.imageUri != null
                ? Uri.parse(contatoExistente.imageUri)
                : null;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_contact, null);
        EditText editName = dialogView.findViewById(R.id.editContactName);
        EditText editPhone = dialogView.findViewById(R.id.editContactPhone);
        imagePreview = dialogView.findViewById(R.id.imageContactPreview);

        editName.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});

        if (contatoExistente != null) {
            editName.setText(contatoExistente.name);
            editPhone.setText(formatarTelefone(contatoExistente.phone));
            if (contatoExistente.imageUri != null)
                imagePreview.setImageURI(Uri.parse(contatoExistente.imageUri));
            else
                imagePreview.setImageResource(R.drawable.add_image);
        } else {
            imagePreview.setImageResource(R.drawable.add_image);
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 200);
        }

        editPhone.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        editPhone.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
        adicionarMascaraTelefone(editPhone);

        imagePreview.setOnClickListener(v -> abrirGaleria());

        new AlertDialog.Builder(requireContext())
                .setTitle(contatoExistente == null ? "Adicionar contato de emergência" : "Editar contato")
                .setView(dialogView)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    String phone = editPhone.getText().toString().replaceAll("\\D", "");

                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Informe o nome.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (phone.length() < 10) {
                        Toast.makeText(getContext(), "Número de telefone inválido.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (contatoExistente == null) {
                        EmergencyContact novo = new EmergencyContact(
                                name,
                                phone,
                                imagemSelecionada != null ? imagemSelecionada.toString() : null,
                                R.drawable.default_contact_image
                        );
                        customContacts.add(novo);
                        Toast.makeText(getContext(), "Contato adicionado!", Toast.LENGTH_SHORT).show();
                    } else {
                        contatoExistente.name = name;
                        contatoExistente.phone = phone;
                        contatoExistente.imageUri = imagemSelecionada != null ? imagemSelecionada.toString() : null;
                        Toast.makeText(getContext(), "Contato atualizado!", Toast.LENGTH_SHORT).show();
                    }

                    salvarContatosPersonalizados();
                    adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        seletorImagemLauncher.launch(intent);
    }

    private void adicionarMascaraTelefone(EditText campo) {
        campo.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdating) return;
                isUpdating = true;

                String digits = s.toString().replaceAll("\\D", "");
                StringBuilder masked = new StringBuilder();

                if (!digits.isEmpty()) masked.append("(");
                if (!digits.isEmpty())
                    masked.append(digits.substring(0, Math.min(2, digits.length())));
                if (digits.length() >= 3)
                    masked.append(") ");
                if (digits.length() > 2 && digits.length() <= 6)
                    masked.append(digits.substring(2));
                else if (digits.length() > 6) {
                    masked.append(digits.substring(2, 7)).append("-");
                    masked.append(digits.substring(7, Math.min(11, digits.length())));
                }

                campo.setText(masked.toString());
                campo.setSelection(masked.length());
                isUpdating = false;
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private String formatarTelefone(String digits) {
        if (digits == null) return "";
        if (digits.length() == 10)
            return String.format("(%s) %s-%s", digits.substring(0, 2), digits.substring(2, 6), digits.substring(6));
        else if (digits.length() == 11)
            return String.format("(%s) %s-%s", digits.substring(0, 2), digits.substring(2, 7), digits.substring(7));
        return digits;
    }

    private void salvarContatosPersonalizados() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = new Gson().toJson(customContacts);
        editor.putString(KEY_CUSTOM_CONTACTS, json);
        editor.apply();
    }

    private void carregarContatosPersonalizados() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CUSTOM_CONTACTS, null);
        if (json != null) {
            Type listType = new TypeToken<ArrayList<EmergencyContact>>() {}.getType();
            List<EmergencyContact> savedContacts = new Gson().fromJson(json, listType);
            if (savedContacts != null) customContacts.addAll(savedContacts);
        }
    }

    public String getNumeroPendendeLigacao() {
        return numeroPendendeLigacao;
    }

    public void setNumeroPendendeLigacao(String numeroPendendeLigacao) {
        this.numeroPendendeLigacao = numeroPendendeLigacao;
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
            EmergencyContact contact = position < contacts.size()
                    ? contacts.get(position)
                    : customContacts.get(position - contacts.size());

            holder.name.setText(contact.name);

            // Sempre define a imagem padrão
            holder.icon.setImageResource(R.drawable.default_contact_image);

            // Contatos fixos com ícones
            if (position < contacts.size()) {
                holder.icon.setImageResource(contact.iconRes);
            }
            // Contatos personalizados com imagem selecionada
            else if (contact.imageUri != null) {
                try {
                    holder.icon.setImageURI(Uri.parse(contact.imageUri));
                } catch (Exception e) {
                    holder.icon.setImageResource(R.drawable.default_contact_image);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED) {
                    numeroPendendeLigacao = contact.phone;
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 100);
                } else {
                    ligarNumero(contact.phone);
                }
            });

            if (position >= contacts.size()) {
                holder.itemView.setOnLongClickListener(v -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mostrarDialogoContato(customContacts.get(position - contacts.size()));
                    }
                    return true;
                });
            }
        }

        @Override
        public int getItemCount() {
            return contacts.size() + customContacts.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView name;
            ShapeableImageView icon;
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

    private static class EmergencyContact {
        String name;
        String phone;
        String imageUri;
        final int iconRes;
        EmergencyContact(String name, String phone, String imageUri, int iconRes) {
            this.name = name;
            this.phone = phone;
            this.imageUri = imageUri;
            this.iconRes = iconRes;
        }
    }
}

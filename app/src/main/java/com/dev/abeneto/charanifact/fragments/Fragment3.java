package com.dev.abeneto.charanifact.fragments;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.dev.abeneto.charanifact.R;
import com.dev.abeneto.charanifact.activities.MainActivity;
import com.dev.abeneto.charanifact.adapter.LineaFacturaAdapter;
import com.dev.abeneto.charanifact.constants.FacturacioConstants;
import com.dev.abeneto.charanifact.db.DatabaseHelper;
import com.dev.abeneto.charanifact.pojo.GastosLaboratori;
import com.dev.abeneto.charanifact.pojo.LineaFactura;
import com.dev.abeneto.charanifact.utils.Utils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alberto on 11/09/2016.
 */
public class Fragment3 extends Fragment {

    private View inflated = null;
    private DatabaseHelper dbHelper = null;
    private ArrayList<LineaFactura> lineasDeFactura = null;
    private static Menu miMenu = null;
    private ListView lineasFacturaListView;
    private LineaFacturaAdapter adapter;


    public Fragment3() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        this.inflated = inflater.inflate(R.layout.fragment_fragment3, container, false);
        this.dbHelper = new DatabaseHelper(getActivity().getApplicationContext());

        MainActivity activity = (MainActivity) getActivity();
        this.miMenu = activity.getMiMenu();

        this.miMenu.findItem(R.id.delete).setVisible(true);

        this.cargarLineasFactura();

        Button botonExcel = (Button) inflated.findViewById(R.id.botonGenerarFacturaExcel);
        Button botonPdf = (Button) inflated.findViewById(R.id.botonGenerarFacturaPdf);

        botonExcel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    generarFactura();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        botonPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Esta funcionalidad aun no está implementada.", Toast.LENGTH_SHORT).show();
            }
        });

        return inflated;
    }

    /**
     * Metodo que carga las lineas de factura del mes actual.
     */
    private void cargarLineasFactura() {
        this.lineasDeFactura = null;
        try {
            Calendar calIni = Calendar.getInstance();
            this.lineasDeFactura = (ArrayList<LineaFactura>) dbHelper.getLineasFacturaOfMonth(calIni.get(Calendar.MONTH), calIni.get(Calendar.YEAR));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (this.lineasDeFactura == null) {
            this.lineasDeFactura = new ArrayList<LineaFactura>();
        } else {
            Collections.sort(lineasDeFactura, new Comparator<LineaFactura>() {
                @Override
                public int compare(LineaFactura lf1, LineaFactura lf2) {
                    return lf1.getFecha().compareTo(lf2.getFecha());
                }
            });
        }
        // Create the adapter to convert the array to views
        this.adapter = new LineaFacturaAdapter(inflated.getContext(), lineasDeFactura);
        // Attach the adapter to a ListView
        this.lineasFacturaListView = (ListView) inflated.findViewById(R.id.listaLineasFactura);
        this.lineasFacturaListView.setAdapter(adapter);
        this.lineasFacturaListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.clear();
        inflater.inflate(R.menu.menu_main, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.delete:
                // EITHER CALL THE METHOD HERE OR DO THE FUNCTION DIRECTLY
                this.deleteFacturaLinea();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Metodo para eliminar las lineas de factura que se seleccionen.
     */
    public void deleteFacturaLinea() {

        SparseBooleanArray elementosSelecctionados = lineasFacturaListView.getCheckedItemPositions();
        List<LineaFactura> lineasFacturaSelected = new ArrayList<>();

        for (int i = 0; i < elementosSelecctionados.size(); i++) {
            if (Boolean.TRUE.equals(elementosSelecctionados.get(elementosSelecctionados.keyAt(i)))) {
                lineasFacturaSelected.add(lineasDeFactura.get(elementosSelecctionados.keyAt(i)));
            }
        }

        if (!lineasFacturaSelected.isEmpty()) {

            try {
                dbHelper.getLineaFacturaDao().delete(lineasFacturaSelected);

                cargarLineasFactura();

                this.adapter.notifyDataSetChanged();

                inflated.invalidate();
            } catch (SQLException e) {
                Toast.makeText(getActivity(), getString(R.string.toast_message_lineas_factura_error), Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(getActivity(), getString(R.string.toast_message_lineas_factura_eliminadas), Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(getActivity(), getString(R.string.toast_message_lineas_factura_ninguna_seleccionada), Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Metodo para generar la factura con las lineas de factura actuales en formato xls
     *
     * @throws IOException
     */
    private void generarFactura() throws IOException {
        InputStream is = getActivity().getApplicationContext().getResources().openRawResource(R.raw.templatefactura);

        Workbook wb = new HSSFWorkbook(is);

        Sheet sheet = wb.getSheetAt(0);

        Cell cell = null;

        Calendar gc = GregorianCalendar.getInstance();
        int mes = gc.get(Calendar.MONTH) + 1;
        int anyo = gc.get(Calendar.YEAR);

        Row filaTitulo = sheet.getRow(0);
        Cell celdaTitulo = filaTitulo.createCell(0);
        celdaTitulo.setCellValue("Facturación de " + Utils.getMonthForInt(mes - 1) + " de " + anyo + "");
        celdaTitulo.getCellStyle().setAlignment(HorizontalAlignment.CENTER);

        int rownum = 2;
        if (lineasDeFactura != null && !lineasDeFactura.isEmpty()) {
            for (LineaFactura lineaFact : lineasDeFactura) {
                Row row = sheet.createRow(rownum++);
                int cellnum = 0;
                SimpleDateFormat sdf = new SimpleDateFormat(FacturacioConstants.SIMPLE_DATE_FORMAT_PATTERN);
                sdf.format(lineaFact.getFecha());
                cell = row.createCell(cellnum++);
                cell.setCellValue(sdf.format(lineaFact.getFecha()));

                cell = row.createCell(cellnum++);
                cell.setCellValue(lineaFact.getClinica().getNumero());

                cell = row.createCell(cellnum++);
                cell.setCellValue(lineaFact.getPacient().toString());

                cell = row.createCell(cellnum++);
                cell.setCellValue(lineaFact.getPacient().getNumeroHistoria());

                cell = row.createCell(cellnum++);
                cell.setCellValue(lineaFact.getTractament().getTipoTractamentEnum().getLabel() + " " + lineaFact.getObservaciones());

                cell = row.createCell(cellnum++);
                cell.setCellValue(this.formatBigDecimal(lineaFact.getImporte()));

                cell = row.createCell(cellnum++);
                cell.setCellValue(lineaFact.getTipusPagament().getCodi());
            }
        }

        Map<String, Object> fieldsGastos = new HashMap<>();
        fieldsGastos.put("mes", mes);
        fieldsGastos.put("any", anyo);

        GastosLaboratori gastosLaboratori = null;

        try {
            gastosLaboratori = dbHelper.getGastosByFields(fieldsGastos);

            if (gastosLaboratori != null) {

                rownum += 2;

                Row row = sheet.createRow(rownum++);
                int cellnum = 0;

                cell = row.createCell(cellnum++);
                cell.setCellValue(getString(R.string.excel_label_gastos_laboratorio));

                row = sheet.createRow(rownum++);
                cellnum = 0;

                cell = row.createCell(cellnum++);
                cell.setCellValue(getString(R.string.excel_label_gastos_laboratorio_orto));

                cell = row.createCell(cellnum++);
                cell.setCellValue(this.formatBigDecimal(gastosLaboratori.getLabOrtodoncia()));


                row = sheet.createRow(rownum++);
                cellnum = 0;

                cell = row.createCell(cellnum++);
                cell.setCellValue(getString(R.string.excel_label_gastos_laboratorio_resi));

                cell = row.createCell(cellnum++);
                cell.setCellValue(this.formatBigDecimal(gastosLaboratori.getLabResitecnic()));

                row = sheet.createRow(rownum++);
                cellnum = 0;

                cell = row.createCell(cellnum++);
                cell.setCellValue(getString(R.string.excel_label_gastos_laboratorio_system));

                cell = row.createCell(cellnum++);
                cell.setCellValue(this.formatBigDecimal(gastosLaboratori.getLabSystem()));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (gastosLaboratori != null) {
            BigDecimal totalFactura = Utils.calcularTotalFactura(lineasDeFactura, gastosLaboratori);

            rownum += 2;

            Row row = sheet.createRow(rownum++);
            int cellnum = 0;

            cell = row.createCell(cellnum++);
            cell.setCellValue(getString(R.string.total_factura));

            cell = row.createCell(cellnum++);
            cell.setCellValue(this.formatBigDecimal(totalFactura));


            rownum += 2;

            row = sheet.createRow(rownum++);
            cellnum = 0;

            cell = row.createCell(cellnum++);
            cell.setCellValue(getString(R.string.total_factura_menos_siete_porciento));

            cell = row.createCell(cellnum++);
            cell.setCellValue(this.formatBigDecimal(totalFactura.multiply(BigDecimal.valueOf(0.93))));

            try {

                String root = Environment.getExternalStorageDirectory().toString();
                File myDir = new File(root + "/saved_facturas");
                myDir.mkdirs();

                String fileName = "Factura_" + (mes - 1L) + "_" + anyo+"_"+ System.currentTimeMillis() + ".xls";

                File file = new File(myDir, fileName);

                FileOutputStream out = new FileOutputStream(file);
                wb.write(out);
                out.flush();
                out.close();

                Toast.makeText(getActivity(), "Fichero excel generado correctamente", Toast.LENGTH_SHORT).show();

                this.viewExcel(Uri.fromFile(file));

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getActivity(), "No se han insertado los gastos de laboratorio del mes actual.", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewExcel(Uri file) {
        Intent intent;
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(file, "application/vnd.ms-excel");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("No Application Found");
            builder.setMessage("Download one from Android Market?");
            builder.setPositiveButton("Yes, Please",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setData(Uri.parse("market://details?id=com.adobe.reader"));
                            startActivity(marketIntent);
                        }
                    });
            builder.setNegativeButton("No, Thanks", null);
            builder.create().show();
        }
    }

    private String formatBigDecimal(BigDecimal numero) {
        DecimalFormat df = new DecimalFormat();

        df.setMaximumFractionDigits(2);

        df.setMinimumFractionDigits(0);

        df.setGroupingUsed(false);

        String result = df.format(numero);

        return result;
    }


}

package org.protege.editor.owl.rdf;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFRow;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.DefaultTableModel;
import org.protege.editor.owl.OWLEditorKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import org.jdesktop.swingx.JXTable;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import org.protege.editor.owl.rdf.repository.BasicSparqlReasonerFactory;

/*
@Author Daniele Francesco Santamaria 
 Department of Mathematics and Computer Science, University of Catania
*/

public class SparqlQueryViewPanel extends JPanel 
  {  
     static final Logger log = LoggerFactory.getLogger(SparqlQueryViewPanel.class);
     SparqlReasoner reasoner;
     JTextPane queryArea;
     JXTable outArea;
     SPARQLResultTableModel model;
     JPanel buttonArea;
     JPanel southPanel;
     JScrollPane scrollNorthArea;
     JScrollPane scrollSouthArea;     
     JSplitPane splitter;
     JButton execute;
     JButton export;
     JButton exportOpz;
     JButton importQ;
     JButton saveQ;
     OptionDialog optionD;   
     String defaultText="PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+"\n"+
                        "PREFIX owl: <http://www.w3.org/2002/07/owl#>" +"\n"+
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" +"\n" +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +"\n" +
                        "SELECT ?subject ?object" +"\n" +
                        " WHERE { ?subject rdfs:subClassOf ?object }";
     OWLEditorKit editorKit;
     OptionConfig optionConfig;
     String [] bnodeVal;
     ProgressWindow wp; 
     SPARQLQueryDocument syntaxHL;
     char[] sparqlDelimiter=new char[]{'}','{','<','>'};
    public SparqlQueryViewPanel(OWLEditorKit kit)
     {     
        wp=new ProgressWindow(null, true); 
        bnodeVal=new String[]{"http://protege.org/owl2triplestore.owl"};
        optionConfig=new OptionConfig();
        editorKit=kit;        
        List<SparqlInferenceFactory> plugins = Collections.singletonList((SparqlInferenceFactory) new BasicSparqlReasonerFactory());
	reasoner = plugins.iterator().next().createReasoner(editorKit.getOWLModelManager().getOWLOntologyManager());
         try
           {
             reasoner.precalculate();
           } catch (SparqlReasonerException ex)
           {
             log.info(ex.toString());
           }
        setLayout(new GridLayout(1,1));
        queryArea=new JTextPane();        
        scrollNorthArea=new JScrollPane(queryArea);
        
        execute= new JButton("Execute Query");
        execute.addActionListener(new ExecuteActionListener());
        export= new JButton("Export Results");
        export.addActionListener(new ExportActionListener());
        exportOpz = new JButton("Options");
        exportOpz.addActionListener(new OptionActionListener());
        importQ = new JButton("Import Query");
        importQ.addActionListener(new ImportActionListener());
        saveQ=new JButton("Save Query");
        saveQ.addActionListener(new SaveActionListener());
        buttonArea = new JPanel(new FlowLayout());
        buttonArea.add(execute);
        buttonArea.add(saveQ);        
        buttonArea.add(importQ);
        buttonArea.add(export);
        buttonArea.add(exportOpz);
            
        model = new SPARQLResultTableModel(0, 2);
        model.setColumnIdentifiers(new String[]{"?subject","?object"});
        outArea=new JXTable(model){
                        @Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
			 Component c = super.prepareRenderer(renderer, row, column);
			 if (!isRowSelected(row))
			  c.setBackground(row % 2 == 0 ? getBackground() : Color.LIGHT_GRAY);
  			 return c;
			}
		};
     
        scrollSouthArea=new JScrollPane(outArea);
        outArea.setHorizontalScrollEnabled(true);
        outArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
              {       
                   TableCellEditor cellEditor = outArea.getCellEditor();
                    if (cellEditor != null) {
                        if (!cellEditor.stopCellEditing()) {
                            cellEditor.cancelCellEditing();
                        }
                    }
                    Component gotFocus = e.getOppositeComponent();                    
                     if (gotFocus!=null && ! (gotFocus.equals(outArea) || gotFocus.equals(export) )) 
                      {
                        outArea.clearSelection(); 
                        outArea.revalidate();
                        outArea.repaint();
                      }
              }           
                 });
        outArea.registerKeyboardAction( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(outArea.getSelectedRowCount()>0)
                {                  
                  String result="";
                  for(int i=0; i< outArea.getSelectedRowCount();i++)
                  { 
                     StringBuilder build=new StringBuilder();
                     for( int j=0; j<outArea.getColumnCount();j++) 
                     {
                       build.append(outArea.getValueAt( outArea.getSelectedRows()[i], j));
                       if(j!=outArea.getColumnCount()-1)
                           build.append(" ");
                     }
                     if(i!=outArea.getSelectedRowCount()-1)
                         build.append("\n");
                     result+=build.toString();
                  }
                  StringSelection selection=new StringSelection(result);
                  Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                }
              }
            } , "Copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.CTRL_DOWN_MASK, false), JComponent.WHEN_FOCUSED);
        
        syntaxHL= new SPARQLQueryDocument();        
        queryArea.setDocument(syntaxHL);
        queryArea.setText(defaultText);
        southPanel=new JPanel(new BorderLayout());        
        southPanel.add(buttonArea, BorderLayout.NORTH);
        southPanel.add(scrollSouthArea, BorderLayout.CENTER);
        
        splitter=new JSplitPane(JSplitPane.VERTICAL_SPLIT,scrollNorthArea, southPanel); 
        splitter.setResizeWeight(0.7);
        add(splitter);       
      
        optionD=new OptionDialog(null);
     } 
    
    
    
    public void dispose() {
       if (reasoner != null) {
			reasoner.dispose();
			reasoner = null;}
    }

     public class SizeFilter extends DocumentFilter {

    private int maxCharacters;    

    public SizeFilter(int maxChars) {
        maxCharacters = maxChars;
    }

    @Override
    public void insertString(FilterBypass fb, int offs, String str, AttributeSet a)
            throws BadLocationException {

        if ((fb.getDocument().getLength() + str.length()) <= maxCharacters && !str.contains("\n"))
            super.insertString(fb, offs, str, a);
        else
            Toolkit.getDefaultToolkit().beep();
    }

    @Override
    public void replace(FilterBypass fb, int offs, int length, String str, AttributeSet a)
            throws BadLocationException {

        if ((fb.getDocument().getLength() + str.length()
                - length) <= maxCharacters && !str.contains("\n"))
            super.replace(fb, offs, length, str, a);
        else
            Toolkit.getDefaultToolkit().beep();
    }
}
     
    class TextOptionPanel extends JPanel
      {
        private JLabel separLabel;
        private JLabel endLabel;
        private JTextArea colSep;
        private JTextArea rowSep;
        public TextOptionPanel()
          {
                 setLayout(new FlowLayout());
                 separLabel=new JLabel("Column Separator");
                 endLabel=new JLabel("Row Separator");
                 colSep=new JTextArea(1,6);
                 colSep.setLineWrap(false);
                 colSep.setWrapStyleWord( false );                
                 ((PlainDocument) colSep.getDocument()).setDocumentFilter(new SizeFilter(6));
                                         
                 rowSep=new JTextArea(1,6);
                 rowSep.setLineWrap(false);
                 rowSep.setWrapStyleWord( false );                
                 ((PlainDocument) rowSep.getDocument()).setDocumentFilter(new SizeFilter(6));
                 
                 //setLayout(new FlowLayout());
                 add(separLabel);
                 add(colSep);
                 add(endLabel);
                 add(rowSep);
          }
        public String[] getParameters(){return new String[]{colSep.getText(), rowSep.getText()};}
      }
    
    class ImageOptionPanel extends JPanel
      {              
        private JComboBox selection;
        private JLabel label;
        public ImageOptionPanel()
          {
                 selection=new JComboBox(new String[]{"JPG","PNG"});
                 selection.setPreferredSize(new Dimension(200, selection.getPreferredSize().height));
                 label=new JLabel("Image format");
                 setLayout(new FlowLayout(FlowLayout.LEFT));     
                 add(label);
                 add(selection);                 
          }
        public String getSelection(){return selection.getSelectedItem().toString();}
      }
    
    class ErrorQueryMessage extends JDialog
      {
          private JPanel envir;
          private JButton okbut;
          public ErrorQueryMessage(String message, String title)
            {
               setTitle(title);
               setVisible(false);         
               setModal(true);                
               setSize(400,200);
               setPreferredSize(new Dimension(400,200));
               setResizable(false);
               envir=new JPanel();
               envir.setLayout(new BorderLayout());
               JTextPane tex=new JTextPane();
               tex.setText(message);
               tex.setEditable(false);
                            
               addWindowListener(new WindowAdapter() {
                     @Override
                     public void windowClosing(WindowEvent e) {
                                    dispose(); //do something
                         }});
               setDefaultCloseOperation(DISPOSE_ON_CLOSE);
               
               okbut=new JButton("OK");
               okbut.addActionListener(new ActionListener(){
                                        @Override
                                        public void actionPerformed(ActionEvent e)
                                          {                                            
                                            dispose();
                                          }
                                   });
               envir.add(new JScrollPane(tex),BorderLayout.CENTER);
               JPanel buttonPanel=new JPanel(new FlowLayout());
               buttonPanel.add(okbut);
               envir.add(buttonPanel,BorderLayout.SOUTH);
               add(envir);
               setLocationRelativeTo(null);
            }          
      }
   

  class OptionActionListener implements ActionListener
    {

        @Override
        public void actionPerformed(ActionEvent e)
          { 
           optionD.setVisible(true);
           
          }
    
    }
    
   public int isBnode(String tomatch)
     {
       for(int i=0; i< bnodeVal.length; i++)
         {
           if(tomatch.startsWith(bnodeVal[i]))
               return bnodeVal[i].length();
         
         }
       return 0;
     }
   
  
    class ExportActionListener implements ActionListener
    {

        private void toJSON(JTable table, int[] iterator, BufferedWriter bw) throws IOException
          {            
            StringBuilder result = new StringBuilder();
            result.append("{" + "\"head\": {");
            result.append("\"vars\": [");
            for(int i=0; i<table.getColumnCount(); i++)
              {
                result.append("\"").append(table.getColumnName(i)).append("\"");
                if(i!=table.getColumnCount()-1)
                   result.append(",");
              }
            result.append("]"); //close vars
            result.append("},"); //close head
            result.append("\"results\": { ");
            result.append(" \"bindings\": ["); //open bindings
            bw.write(result.toString());
            for(int i=0; i < iterator.length; i++)
               {
                result=new StringBuilder();
                result.append("{");
                for(int j=0; j < table.getColumnCount(); j++)
                 {
                   result.append("\"").append(table.getColumnName(j)).append("\":");
                   result.append("{");
                   result.append("\"type\":");                   
                   if(!table.getValueAt( iterator[i], j).toString().startsWith("\""))
                      {
                        int c=isBnode(table.getValueAt(iterator[i], j).toString());
                        if(c>0)
                          {
                            result.append("\"bnode\",\"value\":");
                            result.append("\"").append(table.getValueAt(iterator[i], j).toString()).append("\"");
                          }
                        else
                         {
                          result.append("\"uri\",\"value\":\"");                          
                          result.append(table.getValueAt(iterator[i], j).toString()).append("\"");
                         }
                      }                 
                    else
                      {                      
                        result.append("\"literal\",\"value\":"); 
                        String value= table.getValueAt(iterator[i], j).toString();                         
                        int dataMark= value.indexOf("^");
                        String type="";
                        int initMark=dataMark+3;
                        int finMark=1;
                        if(dataMark>=0)       
                          {
                            type="datatype";
                          }
                        else
                          {
                            dataMark=value.indexOf("@"); 
                            type="xml:lang";
                            initMark=dataMark+1;
                            finMark=0;
                          }
                           String tmp=value.substring(0, dataMark);                            
                           result.append(tmp);
                        
                           result.append(",\"").append(type).append("\":\"");
                           result.append(value.substring(initMark, value.length()-finMark));
                           result.append("\"");
                          
                       }
                     result.append("}");
                     if(j!=table.getColumnCount()-1)
                     result.append(",");
                  }
                 result.append("}");
                 if(i!=iterator.length-1)
                   result.append(",");
                 bw.write(result.toString());
               }
                       
            bw.write("]}}");            
          }
        private void toExcel(JTable table, int[] iterator, String name, FileOutputStream fileOut) throws IOException
          {
            // FileWriter fw = new FileWriter(chooser.getSelectedFile()+".xls");
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet(name);  
            for(int i=0; i < iterator.length; i++)
              {
               HSSFRow rowhead = sheet.createRow((short)i);
               for(int j=0; j < table.getColumnCount(); j++)
                {
                  rowhead.createCell(j).setCellValue(table.getValueAt(iterator[i],j).toString());                                       
                }                    
              }
            for(int i=0; i< table.getColumnCount(); i++)
             sheet.autoSizeColumn(i); 
                               
            workbook.write(fileOut);                              
          }
        private void toSimpleText(JTable table, int [] iterator, BufferedWriter bw) throws IOException
          {
            StringBuilder result = new StringBuilder();            
            for(int i=0; i<table.getColumnCount(); i++)
              {
                result.append(table.getColumnName(i));
                if(i<table.getColumnCount()-1)
                    result.append(optionConfig.param[0]);
              }
            result.append(optionConfig.param[1]);
            bw.write(result.toString());
           
            for(int i=0; i < iterator.length; i++)
               { 
                result=new StringBuilder();               
                for(int j=0; j < table.getColumnCount(); j++)
                 {
                   result.append(table.getValueAt(iterator[i], j));
                   if(j<table.getColumnCount()-1)
                       result.append(optionConfig.param[0]);
                 }
                result.append(optionConfig.param[1]);
                bw.write(result.toString());
               }
          }

        public BufferedImage createImage(JTable table) 
      {
        JTableHeader tableHeaderComp = table.getTableHeader();
        int totalWidth = tableHeaderComp.getWidth() + table.getWidth();
        int totalHeight = tableHeaderComp.getHeight() + table.getHeight();
        BufferedImage tableImage = new BufferedImage(totalWidth, totalHeight,
        BufferedImage.TYPE_INT_RGB);
        Graphics2D g2D = (Graphics2D) tableImage.getGraphics();
        tableHeaderComp.paint(g2D);
        g2D.translate(0, tableHeaderComp.getHeight());
        table.paint(g2D);
        return tableImage;
       }
        
        @Override
        public void actionPerformed(ActionEvent e)
          { 
            if(model.getDataVector().isEmpty())
                 {
                   JOptionPane.showMessageDialog(null, "No query executed. Execute a query.");
                   return;
                 }
            JFileChooser chooser = new JFileChooser();     
            int retrival = chooser.showSaveDialog(null);
            if (retrival == JFileChooser.APPROVE_OPTION)
             {     
              // ProgressWorker pbarView=new ProgressWorker("Executing","Running Operation");
             //  pbarView.execute();
              SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
              @Override
               protected String doInBackground(){   
              int iterator[];
              if(outArea.getSelectedRowCount()>0)                
                {
                  iterator=new int[outArea.getSelectedRowCount()];
                  for(int i=0;i<outArea.getSelectedRowCount();i++)
                    iterator[i]= outArea.convertRowIndexToModel(outArea.getSelectedRows()[i]);
                }
              else
                {
                  iterator=new int[outArea.getRowCount()];
                  for(int i=0; i<outArea.getRowCount();i++)
                   iterator[i]=i;
                }
                
              try 
               {
                  log.info("Exporting results..."); 
                  switch (optionConfig.format)
                     {
                          case 0:
                             {  
                               FileOutputStream fileout = new FileOutputStream(chooser.getSelectedFile()+".xls");
                               toExcel(outArea, iterator, chooser.getSelectedFile().getName(), fileout);                             
                               fileout.close();
                               break;                       
                             }                     
                          case 1: 
                              {     
                                BufferedWriter bw = 
                                    new BufferedWriter(new FileWriter(chooser.getSelectedFile()+".srj", true));
                                toJSON(outArea, iterator, bw);                                 
                                bw.close();
                                break;  
                              }
                          case 2:
                            {
                                BufferedWriter bw = 
                                    new BufferedWriter(new FileWriter(chooser.getSelectedFile(), true));
                                toSimpleText(outArea, iterator, bw);                                 
                                bw.close();
                                break;
                            }
                          case 3:
                            {
                              BufferedImage imageout= createImage(outArea);
                              File outputfile = new File(chooser.getSelectedFile()+"."+optionD.imgOpP.getSelection());
                              ImageIO.write(imageout, optionD.imgOpP.getSelection(), outputfile);
                              break;
                            }
                          default: break;
                    }
                  log.info("Exporting successed.");
                 }
                 catch (IOException ex)
                   {
                     done();
                     log.error("Error on exporting results:" + ex.toString());                     
                     ErrorQueryMessage messaged=new ErrorQueryMessage(ex.toString(), "Error on exporting results");           
                     messaged.setVisible(true);
                   }
                return "done";
                        }
               
                 @Override
                  protected void done() {
                  wp.dispose();            
                   }
              };
              worker.execute();
              wp.setVisible(true);
             }           
          }    
    }
    
  class SaveActionListener implements ActionListener
  {    
    @Override
    public void actionPerformed(ActionEvent event)
      {
       JFileChooser chooser = new JFileChooser();       
      int retrival = chooser.showSaveDialog(null);
      if (retrival == JFileChooser.APPROVE_OPTION)
        {
         try 
          {
            log.info("Saving Query..."); 
            FileWriter fw = new FileWriter(chooser.getSelectedFile()+".SPARQL");
            fw.write(queryArea.getText());
            fw.close();
          } 
         catch (IOException ex)
           {
             log.error("Error on writing Query on file. "+ex.toString());
             ErrorQueryMessage messaged=new ErrorQueryMessage(ex.toString(), "Error on saving query");           
             messaged.setVisible(true);
           }
        }
      }
    }
  
   class ImportActionListener implements ActionListener
   {    
    @Override
    public void actionPerformed(ActionEvent event)
      {
       JFileChooser chooser = new JFileChooser();       
      int retrival = chooser.showOpenDialog(null);
      if (retrival == JFileChooser.APPROVE_OPTION)
        {
         try 
          {
            log.info("Importing query..."); 
            FileReader fr = new FileReader(chooser.getSelectedFile());
	    BufferedReader br = new BufferedReader(fr);
   	    String sCurrentLine=null;
            String fstring="";
	    while ((sCurrentLine = br.readLine()) != null)             
	       fstring+=sCurrentLine+"\n";
	    queryArea.setText(fstring);

          } 
         catch (IOException ex)
           {
             log.error("Error on reading Query file. "+ ex.toString());
             ErrorQueryMessage messaged=new ErrorQueryMessage(ex.toString(), "Error on reading query");           
             messaged.setVisible(true);
           }
        }
      }   
     }
   
   class ExecuteActionListener implements ActionListener
   {   
        private String[][] createData(SparqlResultSet set)
          {
            String data[][]=new String[set.getRowCount()][set.getColumnCount()];
            for(int i=0; i<set.getColumnCount(); i++)
                for(int j=0; j<set.getRowCount(); j++)
                    data[j][i]=(set.getResult(j,i).toString());
            return data;
          }
     
    @Override
    public void actionPerformed(ActionEvent event)
      {                 
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground(){          
        try
          {             
            log.info("Executing query..."); 
            SparqlResultSet set = reasoner.executeQuery(queryArea.getText());                      
            String data[][];              
            
            String names[]=new String[set.getColumnCount()];        
            for(int i=0; i<set.getColumnCount();i++)
                names[i]=set.getColumnName(i);
            
            data=createData(set);
            model.setDataVector(data, names);
            model.fireTableDataChanged();   
            outArea.revalidate();
            outArea.repaint(); 
            log.info("Query execution successed.");
           
          } 
        catch (SparqlReasonerException ex)
          {   
           done();
           log.info("Error on executing query. "+ ex.toString());
           ErrorQueryMessage messaged=new ErrorQueryMessage(ex.toString(), "Error on executing query");
           messaged.setResizable(true);
           messaged.setVisible(true);           
          }
        return "Done";
        }
        @Override
        protected void done() {
            wp.dispose();            
        }
        };
        worker.execute();
        wp.setVisible(true);
      }
  }

   
public class OptionDialog extends JDialog
  {
    JPanel topPanel;
    JPanel bottomPanel;
    JComboBox formatBox;
    JButton okbutton;
    TextOptionPanel textOpP;
    ImageOptionPanel imgOpP;
    JPanel formatOptionContainer;
       
    public OptionDialog (JFrame parent)
     {
         super(parent, "Options");
         setSize(320,170);
         setPreferredSize(new Dimension(320,170));
         setVisible(false);         
         setModal(true);        
         setLayout(new BorderLayout());
         //setResizable(false);
         addWindowListener(new WindowAdapter() {
                     @Override
                     public void windowClosing(WindowEvent e) {
                                    closeDiscard(); //do something
                         }});
         
         
         formatBox=new JComboBox(new String[]{"Microsoft Excel", "SPARQL JSON", "Simple Text", "Image"});
         formatBox.setPreferredSize(new Dimension(200, formatBox.getPreferredSize().height));
         
         
         optionConfig=new OptionConfig(formatBox.getSelectedIndex());
         
         okbutton=new JButton("OK");                 
         
         topPanel=new JPanel();
         topPanel.setLayout(new FlowLayout());
         topPanel.add(new JLabel("Export Format"));
         topPanel.add(formatBox);
         
         bottomPanel=new JPanel();
         bottomPanel.setLayout(new FlowLayout());
         bottomPanel.add(okbutton);
         
        textOpP=new TextOptionPanel();
        imgOpP=new ImageOptionPanel();
        formatOptionContainer =new JPanel();
         
         add(topPanel, BorderLayout.NORTH);
         add(bottomPanel, BorderLayout.SOUTH);
         add(formatOptionContainer);
         
         formatBox.addItemListener(new FormatActionListener(this));
         okbutton.addActionListener(new ActionListener(){
                                        @Override
                                        public void actionPerformed(ActionEvent e)
                                          {
                                            if(formatBox.getSelectedIndex()==2)
                                                optionConfig.save(formatBox.getSelectedIndex(),
                                                                  textOpP.getParameters());
                                            
                                            optionConfig.save(formatBox.getSelectedIndex());
                                            dispose();
                                          }
                                   });
         
         setLocationRelativeTo(null);
     }  
    public void closeDiscard()
      {
          formatBox.setSelectedIndex(optionConfig.format);
          dispose();
      }
    
    class FormatActionListener implements ItemListener
      {
        JDialog parent;        
        public FormatActionListener(JDialog _parent)
          {
            parent=_parent;           
          }

             @Override
             public void itemStateChanged(ItemEvent event)
               {
                 if (event.getStateChange() == ItemEvent.SELECTED)
                      {  
                        if(((JComboBox)event.getSource()).getSelectedIndex()==2)                         
                            {
                             formatOptionContainer.add(textOpP);
                            }
                        else if(((JComboBox)event.getSource()).getSelectedIndex()==3)                         
                            {
                             formatOptionContainer.add(imgOpP);
                            }
                       }
                 else {formatOptionContainer.removeAll();}
                 parent.revalidate();                
                 parent.repaint();
               }
      }
  }


     /*
         Thanks to @shuangwhywhy for this class
      */
   class SPARQLQueryDocument extends DefaultStyledDocument
     {
        StyleContext cont;
        AttributeSet attr;
        AttributeSet attrRed;
        AttributeSet attrBlack;
        AttributeSet attrGreen;
        public SPARQLQueryDocument()
          {
            super();
            cont = StyleContext.getDefaultStyleContext();
            attr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, Color.blue);
            attrRed = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, Color.red);
            attrBlack = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, Color.BLACK);
            attrGreen = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, new Color(0,102,0));
          }

//        private boolean findDelimiter(String text, int start, int end, AttributeSet attrRed, boolean b)
//          {
//            for(int i=start; i<end; i++)
//              {
//                for(int j=0; j<sparqlDelimiter.length;j++)
//                  {
//                     if(text.charAt(i)==sparqlDelimiter[j])
//                       {
//                         setCharacterAttributes(i, i, attr, false);
//                         return true;
//                       }
//                  }
//              }
//            return false;
//          }
        @Override
        public void insertString (int offset, String str, AttributeSet a) throws BadLocationException 
          {
            super.insertString(offset, str, a);
            String text = getText(0, getLength());
            int before = findLastNonWordChar(text, offset);
            if (before < 0) 
                before = 0;
            int after = findFirstNonWordChar(text, offset + str.length());
            int wordL = before;
            int wordR = before;
            while (wordR <= after) 
              {
               if (wordR == after || String.valueOf(text.charAt(wordR)).matches("\\s"))
                 {
                   log.info(text.substring(wordL, wordR));
                  if (text.substring(wordL, wordR).matches("(\\s)*(\\{|\\}|PREFIX|DISTINCT|SELECT|WHERE|UNION|FILTER|LIMIT|OFFSET|OPTIONAL|GROUP|ORDER|BY)"))
                       setCharacterAttributes(wordL, wordR - wordL, attr, false);
                 // else if (findDelimiter(text.substring(wordL, wordR), wordL, wordR,attrRed,false))
                 //   {}              
                  else if (text.substring(wordL, wordR).matches("(\\s)*(\\?[\\w]+)"))
                     setCharacterAttributes(wordL, wordR - wordL, attrGreen, false);
                  else if (text.substring(wordL, wordR).matches("(\\s)*(<(.+?)>)"))
                       setCharacterAttributes(wordL, wordR - wordL, attrRed, false);                 
                  else
                     setCharacterAttributes(wordL, wordR - wordL, attrBlack, false);
                  wordL = wordR;
               }
                 wordR++;
              }
            }

        @Override
            public void remove (int offs, int len) throws BadLocationException {
                super.remove(offs, len);

                String text = getText(0, getLength());
                int before = findLastNonWordChar(text, offs);
                if (before < 0) before = 0;
                int after = findFirstNonWordChar(text, offs);

                if(text.substring(before, after).matches("(\\s)*(\\{|\\}|PREFIX|DISTINCT|SELECT|WHERE|UNION|FILTER|LIMIT|OFFSET|OPTIONAL|GROUP|ORDER|BY)"))
                  {
                    setCharacterAttributes(before, after - before, attr, false);
                  } 
                else if (text.substring(before, after).matches("(\\s)*(\\?[\\w]+)"))
                     setCharacterAttributes(before, after - before, attrGreen, false);
                  else if (text.substring(before, after).matches("(\\s)*(<(.+?)>)"))
                       setCharacterAttributes(before, after - before, attrRed, false);                 
                  else 
                  {
                    setCharacterAttributes(before, after - before, attrBlack, false);
                }
            }
            
            private int findLastNonWordChar (String text, int index) {
        while (--index >= 0) {
            if (String.valueOf(text.charAt(index)).matches("\\s")) {
                break;
            }
        }
        return index;
    }
    private int findFirstNonWordChar (String text, int index) {
        while (index < text.length()) {
            if (String.valueOf(text.charAt(index)).matches("\\s")) {
                break;
            }
            index++;
        }
        return index;
    }
        }
}

 class SPARQLResultTableModel extends DefaultTableModel 
   {

    public SPARQLResultTableModel(int x, int y)
      {
         super(x, y);         
          
      }
           
    public boolean isCellEditable() {
          return false;
            }
     }

class OptionConfig
  {
     int format;
     String[] param;
     public OptionConfig(){
         format=-1; param=new String[]{};
     }
     public OptionConfig(int value){format=value; param=new String[]{};}
     public void save(int _format){format=_format;}
     public void save(int _format, String[] _param)
       {
         save(_format);
         param=_param;
       }
  }

class ProgressWindow extends javax.swing.JDialog
  {
    /**
     * Creates new form ProgressWindow
     */
    public ProgressWindow(java.awt.Frame parent, boolean modal)
      {
        super(parent, modal);
        initComponents();
//        try
//          {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
//              {
//                if ("Nimbus".equals(info.getName()))
//                  {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                  }
//              }
//          } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex)
//          {
//            java.util.logging.Logger.getLogger(ProgressWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//          }           
      }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents()
    {

        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);        
        jProgressBar1.setIndeterminate(true);

        jLabel1.setText("Running Operation ...");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        setLocationRelativeTo(null);
        pack();
    }// </editor-fold>                        

    /**
     */
    
    
    // Variables declaration - do not modify                     
    private javax.swing.JLabel jLabel1;
    private javax.swing.JProgressBar jProgressBar1;
    // End of variables declaration                   
  }

 

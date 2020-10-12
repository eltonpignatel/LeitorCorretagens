package br.com.eltonpignatel;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import br.com.eltonpignatel.dao.NotaCorretagemDAO;
import br.com.eltonpignatel.model.NotaCorretagem;
import br.com.eltonpignatel.model.NotaCorretagemLancto;

public class LeitorClear {
	
	public void ler() {
		
		File diretorioArquivos = new File(Leitor.diretorioClear);
		File[] arquivos  = diretorioArquivos.listFiles();
		
		for (File arquivo : arquivos) {
			try {
				processar(arquivo);
				
				Files.move(arquivo.toPath(), Paths.get( Leitor.diretorioProcessados + File.separator +  arquivo.getName() ), StandardCopyOption.REPLACE_EXISTING);
				
			} catch (IOException e) {
				e.printStackTrace();
				try {
					Files.move(arquivo.toPath(), Paths.get( Leitor.diretorioErros + File.separator +  arquivo.getName() ), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Files.move(arquivo.toPath(), Paths.get( Leitor.diretorioErros + File.separator +  arquivo.getName() ), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	private void processar(File arquivo) throws Exception {
		
		String [] linhas;
		
		int posInicialLanctos = 0;
		int posFinalLanctos = 0;
		
		int posInicialResumoFinanceiro = 0;
		int posInicialCustosOperacionais = 0;
		int posInicialResumoDosNegocios = 0; //Vari�vel n�o utilizada mas preenchida no layout para uso futuro
		
		NotaCorretagem notaDeCorretagem = null;;
		NotaCorretagemLancto notaDeCorretagemLancto; 
		List <NotaCorretagemLancto> notaDeCorretagemLanctoList;
		
		Rectangle rect;
		PDDocument document = null;
		
		try {
			document = PDDocument.load(arquivo, "077");
			document.setAllSecurityToBeRemoved(true);
	    	
	        PDFTextStripper stripper;
	        PDFTextStripperByArea stripperArea;
			
			for (int p = 1; p <= document.getNumberOfPages(); ++p) {
				
				stripper = new PDFTextStripper();
				stripper.setSortByPosition(true);
				
	            stripper.setStartPage(p);
	            stripper.setEndPage(p);

	            String text = stripper.getText(document);
	            
	            linhas = text.trim().split(System.lineSeparator());
	            
	            notaDeCorretagem = new NotaCorretagem();
	            notaDeCorretagem.setCorretora(linhas[3]);
	            notaDeCorretagem.setDataPregao(new SimpleDateFormat("dd/MM/yyyy").parse(linhas[2].split(" ")[2]));
	            notaDeCorretagem.setNumeroCorretagem(Integer.parseInt(linhas[2].split(" ")[0]));
	                            
	            //Identifica e categoriza as linhas
	            for (int i = 0; i < linhas.length; i++) {

	            	if (linhas[i].equals("Neg�cios realizados")) {
	            		posInicialLanctos = i+2;
	            	}
	            	
	            	if (linhas[i].equals("Resumo dos Neg�cios Resumo Financeiro")) {
	            		posFinalLanctos = i-1;
	            	}
	            	
	            	if (linhas[i].endsWith("Custos Operacionais")) {
	            		posInicialCustosOperacionais = i;                		
	            	}
	            	
	            	if (linhas[i].startsWith("Resumo dos Neg�cios")) {
	            		posInicialResumoDosNegocios = i;                		
	            	}
	            	
	            	if (linhas[i].endsWith("Resumo Financeiro")) {
	            		posInicialResumoFinanceiro = i;                		
	            	}
	            	
	            }
	            
	            //lanctos
	            int contLanctos = 0;
	            notaDeCorretagemLanctoList = new ArrayList<NotaCorretagemLancto>();
	            
	            for (int i = posInicialLanctos; i <= posFinalLanctos; i++) {

	            	contLanctos++;
	            	
	            	stripperArea = new PDFTextStripperByArea();
	    			stripperArea.setSortByPosition( true );
	    			
	    			//Posi��o do nome do ativo
	    			rect = new Rectangle(155, 245+(contLanctos*10), 140, 12 );
	                stripperArea.addRegion( "lancamentoAtivo", rect );
	                
	                //Posi��o dos valores
	                rect = new Rectangle( 325, 245+(contLanctos*10), 540, 12 );
	                stripperArea.addRegion( "lancamentoValores", rect );
	                stripperArea.extractRegions( document.getPage(p-1) );
	                
	                notaDeCorretagemLancto = new NotaCorretagemLancto();
	                
	                notaDeCorretagemLancto.setNegociacao( linhas[i].split(" ")[0] );
	                notaDeCorretagemLancto.setC_V( linhas[i].split(" ")[1] );
	                notaDeCorretagemLancto.setTipoMercado( linhas[i].split(" ")[2] );
	                notaDeCorretagemLancto.setEspecificacaoTitulo(  stripperArea.getTextForRegion( "lancamentoAtivo" ).replace(System.lineSeparator(), "")  );
	                notaDeCorretagemLancto.setQuantidade( Integer.parseInt( stripperArea.getTextForRegion( "lancamentoValores" ).replace(System.lineSeparator(), "").split(" ")[0] ));
	                notaDeCorretagemLancto.setPrecoAjuste( Double.parseDouble(stripperArea.getTextForRegion( "lancamentoValores" ).replace(System.lineSeparator(), "").split(" ")[1].replace(".", "").replace(",", ".")) );
	                notaDeCorretagemLancto.setValorOperacaoAjuste( Double.parseDouble( stripperArea.getTextForRegion( "lancamentoValores" ).replace(System.lineSeparator(), "").split(" ")[2].replace(".", "").replace(",", ".") ) );
	                notaDeCorretagemLancto.setD_C( stripperArea.getTextForRegion( "lancamentoValores" ).replace(System.lineSeparator(), "").split(" ")[3] );
	                notaDeCorretagemLancto.setFolha(Integer.parseInt(linhas[2].split(" ")[1]));
	                notaDeCorretagemLanctoList.add(notaDeCorretagemLancto);
	                
	            }
	            
	            notaDeCorretagem.setLanctos(notaDeCorretagemLanctoList);
	          
	            //Resumo financeiro
	            for (int i = posInicialResumoFinanceiro; i < posInicialCustosOperacionais; i++) {
	            	
	            	if (linhas[i].contains("Valor l�quido das opera��es") ) {
	            		notaDeCorretagem.setValorLiquidoOperacoes(Double.parseDouble(linhas[i].substring(linhas[i].indexOf("Valor l�quido das opera��es")).split(" ")[4].replace(".", "").replace(",", ".")));
	            	}
	            	
	            	if (linhas[i].contains("Taxa de liquida��o") ) {
	            		notaDeCorretagem.setTaxaLiquidacao(Double.parseDouble(linhas[i].substring(linhas[i].indexOf("Taxa de liquida��o")).split(" ")[3].replace(".", "").replace(",", ".")));
	            	}
	            	
	            	if (linhas[i].contains("Taxa de Registro") ) {
	            		notaDeCorretagem.setTaxaRegistro (Double.parseDouble(linhas[i].substring(linhas[i].indexOf("Taxa de Registro")).split(" ")[3].replace(".", "").replace(",", ".")));
	            	}
	            	
	            	if (linhas[i].contains("Taxa de Registro") ) {
	            		notaDeCorretagem.setTaxaRegistro (Double.parseDouble(linhas[i].substring(linhas[i].indexOf("Taxa de Registro")).split(" ")[3].replace(".", "").replace(",", ".")));
	            	}
	            	
	            	if (linhas[i].contains("Emolumentos") ) {
	            		notaDeCorretagem.setEmolumentos(Double.parseDouble(linhas[i].substring(linhas[i].indexOf("Emolumentos")).split(" ")[1].replace(".", "").replace(",", ".")));
	            	}
	            	
	            }

	            //Custos operacionais
	            for (int i = posInicialCustosOperacionais; i < linhas.length; i++) {
	            	if (linhas[i].contains("I.R.R.F. s/ opera��es") ) {
	            		notaDeCorretagem.setBaseIRRF(Double.parseDouble(linhas[i].substring(linhas[i].indexOf("I.R.R.F. s/ opera��es")).split(" ")[4].replace(".", "").replace(",", ".").replace("R$", "")) );
	            		notaDeCorretagem.setValorIRRF(Double.parseDouble(linhas[i].substring(linhas[i].indexOf("I.R.R.F. s/ opera��es")).split(" ")[5].replace(".", "").replace(",", ".")));
	            	}
	            }
	            
	            new NotaCorretagemDAO().gravarJson(notaDeCorretagem);

	        }

			document.close();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			document.close();
			throw e;
		} finally {
			document.close();
		}
		
    	

	}
}

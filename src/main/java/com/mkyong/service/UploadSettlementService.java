package com.mkyong.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.javalite.http.Http;
import org.javalite.http.Post;
import org.jeecgframework.poi.excel.entity.enmus.CellValueType;
import org.jeecgframework.poi.excel.entity.sax.SaxReadCellEntity;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.mkyong.controller.UploadData;

import javax.xml.parsers.ParserConfigurationException;

public class UploadSettlementService {
	//private long startTime = 0;
	public static long totalCount = 0;
	public static List<List<String>> arrayList = null;
	public static int position = 0;
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	public Future<Long> processAllSheets(String filename, List<String> headerColumns, int headerRow, String type, 
			String auth, String originalFile, String pid)
			throws Exception {
		
		return executor.submit(() -> {
			try {
				OPCPackage pkg = OPCPackage.open(filename);
				XSSFReader r = new XSSFReader(pkg);
				SharedStringsTable sst = r.getSharedStringsTable();

				XMLReader parser;
				try {
					parser = fetchSheetParser(sst, headerColumns, headerRow, type, auth, originalFile, pid);
					
					Iterator<InputStream> sheets = r.getSheetsData();
					//startTime = System.currentTimeMillis();
					while (sheets.hasNext()) {
						System.out.println("Processing new sheet:\n");
						InputStream sheet = sheets.next();
						InputSource sheetSource = new InputSource(sheet);
						parser.parse(sheetSource);
						sheet.close();
						if (totalCount > position) {
							List<List<String>> arrayOfRecord = new ArrayList<>();
							int endPosition = new Long(totalCount).intValue();
							arrayOfRecord = UploadSettlementService.arrayList.subList(position, endPosition - 1);
//							int itemCount = 0;
//							for (int index = position; index < totalCount; index++) {
//								itemCount = index + 1;
//								arrayOfRecord.add(UploadSettlementService.arrayList.get(index));
//							}
							UploadData uploadData = new UploadData();
							uploadData.setPid(pid);
							uploadData.setFilename(originalFile);
							uploadData.setItemCount(totalCount);
							uploadData.setProcessor(type);
							uploadData.setData(arrayOfRecord);
							uploadData.setLast(true);
							UploadSettlementService.sendRecordToServer(uploadData, auth);
							UploadSettlementService.arrayList = null;
						}
						System.out.println("");
					}
					//long endTime = System.currentTimeMillis();
					return totalCount;
				} catch (SAXException | ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (OpenXML4JException e) {
				e.printStackTrace();
			}
			return totalCount;
		});
		
		//return 
	}

	public XMLReader fetchSheetParser(SharedStringsTable sst, List<String> headerColumns, int headerRow,
			String uploadType, String auth, String originalFile, String pid) throws SAXException, ParserConfigurationException {
		XMLReader parser = SAXHelper.newXMLReader();
		ContentHandler handler = new SheetHandler(sst, headerColumns, headerRow, uploadType, auth, 
				originalFile, pid);
		parser.setContentHandler(handler);
		return parser;
	}

	public static void sendRecordToServer(UploadData uploadData, String authorization) {
		try {
			Properties properties = CommonUtil.loadPropertySettings("settings");
			String baseUrl = properties.getProperty("base_url");
			String data = new Gson().toJson(uploadData);
			System.out.println(data);
			Post postData = Http.post(baseUrl + "api/v1/settlements/uploads", data)
					.header("Content-Type", "application/json").header("Authorization", authorization);
			System.out.println(postData.text());
			// reset
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * See org.xml.sax.helpers.DefaultHandler javadocs
	 */
	private static class SheetHandler extends DefaultHandler {
		private SharedStringsTable sst;
		private String lastContents;
		private int curRow = 0;
		private int curCol = 0;
		private CellValueType type;
		private int step = 0;
		private List<List<String>> arrayOfRecords;
		private List<String> headerColumnNames;
		private List<String> bufferedValues;
		private int headerRow;
		private List<SaxReadCellEntity> rowlist = Lists.newArrayList();
		private Gson gson;
		private String[] previousColumns = new String[] { "G", "N", "S", "Z" };
		private String[] nextColumns = new String[] { "I", "J", "R", "U", "AC" };

		private String[] iswPreviousColumns = new String[] { "X" };
		private String[] iswNextColumns = new String[] { "Z" };

		private List<Integer> savedPreviousLocations;
		private List<String> savedNextLetter;

		private List<Integer> iswSavedPreviousLocations;
		private List<String> iswSavedNextLetter;

		private int headerColumns;
		private String uploadType;
		private String authorization;
		private String originalFilename;
		private String pid;
		private long itemCount;
		private boolean isLast = false;
		//private List<List<String>> arrayList = null;

		//private List<Integer> buffer = new ArrayList<Integer>();
		private List<String> values;
		
		private SheetHandler(SharedStringsTable sst, List<String> headerColumns, int headerRow, String processor,
				String authorization, String originalFilename, String pid) {
			UploadSettlementService.arrayList = new ArrayList<List<String>>();
			this.values = new ArrayList<String>(2);
			this.arrayOfRecords = new ArrayList<List<String>>();
			this.headerColumnNames = new ArrayList<String>();
			this.headerColumnNames.addAll(headerColumns);
			this.bufferedValues = new ArrayList<String>(headerColumns.size());
			this.sst = sst;
			this.headerRow = headerRow;
			this.gson = new Gson();
			this.savedPreviousLocations = new ArrayList<Integer>();
			this.savedNextLetter = new ArrayList<String>();
			this.iswSavedPreviousLocations = new ArrayList<Integer>();
			this.iswSavedNextLetter = new ArrayList<String>();
			this.headerColumns = headerColumns.size();
			this.uploadType = processor;
			this.authorization = authorization;
			this.originalFilename = originalFilename;
			this.pid = pid;
		}

		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if (curRow >= headerRow) {
				if (attributes.getValue(0) != null) {
					if (attributes.getValue(0).contains(previousColumns[0])) {
						savedPreviousLocations.add(curCol);
					} else if (attributes.getValue(0).contains(previousColumns[1])) {
						savedPreviousLocations.add(curCol);
					} else if (attributes.getValue(0).contains(previousColumns[2])) {
						savedPreviousLocations.add(curCol);
					} else if (attributes.getValue(0).contains(previousColumns[3])) {
						savedPreviousLocations.add(curCol);
					}

					if (attributes.getValue(0).contains(nextColumns[0])) {
						if (savedNextLetter.contains("J") || savedNextLetter.contains("I")) {
							// do nothing
						} else {
							savedNextLetter.add(nextColumns[0]);
						}
					} else if (attributes.getValue(0).contains(nextColumns[1])) {
						if (savedNextLetter.contains("J") || savedNextLetter.contains("I")) {
							// do nothing
						} else {
							savedNextLetter.add(nextColumns[1]);
						}
					} else if (attributes.getValue(0).contains(nextColumns[2])) {
						savedNextLetter.add(nextColumns[2]);
					} else if (attributes.getValue(0).contains(nextColumns[3])) {
						savedNextLetter.add(nextColumns[3]);
					} else if (attributes.getValue(0).contains(nextColumns[4])) {
						savedNextLetter.add(nextColumns[4]);
					}
				}

				if (attributes.getValue(0) != null) {
					if (attributes.getValue(0).contains(iswPreviousColumns[0]) && headerColumnNames.size() == 33) {
						iswSavedPreviousLocations.add(curCol);
					}
					if (attributes.getValue(0).contains(iswNextColumns[0])) {
						iswSavedNextLetter.add(iswNextColumns[0]);
					}
				}
			}
			// c => cell
			if ("c".equals(name)) {
				// Mark nextIsString as true if the next element is an index to SST
				String cellType = attributes.getValue("t");
				if ("s".equals(cellType)) {
					type = CellValueType.String;
					return;
				}
				// Date format
				cellType = attributes.getValue("s");
				if ("1".equals(cellType)) {
					type = CellValueType.Date;
				} else if ("2".equals(cellType)) {
					type = CellValueType.Number;
				} else if ("13".equals(cellType)) {
					type = CellValueType.Date;
				} else if ("7".equals(cellType)) {
					type = CellValueType.Date;
				} else {
					type = CellValueType.Number;
				}
			} else if ("t".equals(name)) {
				type = CellValueType.TElement;
			}
			// Clear contents cache
			lastContents = "";
		}

		@SuppressWarnings("deprecation")
		public void endElement(String uri, String localName, String name) throws SAXException {
			if (CellValueType.String.equals(type)) {
				try {
					int idx = Integer.parseInt(lastContents);
					lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
				} catch (Exception e) {
				}
			}
			if (CellValueType.TElement.equals(type)) {
				String value = lastContents.trim();
				rowlist.add(curCol, new SaxReadCellEntity(CellValueType.String, value));
				curCol++;
				type = CellValueType.None;
			} else if ("v".equals(name)) {
				// System.out.println("Name: " + name);
				String value = lastContents.trim();
				value = value.equals("") ? "Blank" : value;
				computeBufferedValued(value);
				curCol++;
			} else if (name.equals("row")) {
				bufferedValues = new ArrayList<String>();
				rowlist.clear();
				curRow++;
				step = 0;
				savedPreviousLocations = new ArrayList<Integer>();
				savedNextLetter = new ArrayList<String>();
				iswSavedPreviousLocations = new ArrayList<Integer>();
				iswSavedNextLetter = new ArrayList<String>();
				curCol = 0;
			} else {
				if ("c".equals(name)) {
					if (lastContents.isEmpty()) {
						String value = "Blank".trim();
						value = value.equals("") ? "Blank" : value;
						computeBufferedValued(value);
						curCol++;
					}
				}
			}
		}

		private void computeBufferedValued(String value) {
			if (curRow >= headerRow && !headerColumnNames.contains(value)) {
				bufferedValues.add(value);
				if (bufferedValues.contains("FEES COLLECTED FOR ALL PTSPs")) {
					if (iswSavedPreviousLocations != null && iswSavedNextLetter != null) {
						if (iswSavedPreviousLocations.size() == iswPreviousColumns.length) {
							int startPos = 0;
							int endPos = 0;
							step = 0;
							for (int nextIndex = 0; nextIndex < iswSavedNextLetter.size(); nextIndex++) {
								if (iswSavedNextLetter.get(nextIndex) == "Z") {
									int startPosition = iswSavedPreviousLocations.get(0);
									startPos = startPosition + 1;
									endPos = 25 - 1;
									for (int start = startPos; start <= endPos; start++) {
										if (bufferedValues.size() < headerColumnNames.size()) {
											bufferedValues.add(start, "Blank");
										}
									}
									step = 0;
								}
							}
						}
					}
				}
				if (bufferedValues.contains("GENERAL")) {
					if (savedPreviousLocations != null && savedNextLetter != null) {
						if (savedPreviousLocations.size() == previousColumns.length) {
							int startPos = 0;
							int endPos = 0;
							for (int nextIndex = 0; nextIndex < savedNextLetter.size(); nextIndex++) {
								if (savedNextLetter.get(nextIndex) == "I") {
									int startPosition = savedPreviousLocations.get(0);
									startPos = startPosition + 1;
									endPos = 8 - 1;
									step = 0;
									for (int start = startPos; start <= endPos; start++) {
										bufferedValues.add(start, "Blank");
										++step;
									}
								} else if (savedNextLetter.get(nextIndex) == "J") {
									int startPosition = savedPreviousLocations.get(0);
									startPos = startPosition + 1;
									endPos = 9 - 1;
									step = 0;
									for (int start = startPos; start <= endPos; start++) {
										bufferedValues.add(start, "Blank");
										++step;
									}
								} else if (savedNextLetter.get(nextIndex) == "R") {
									int startPosition = savedPreviousLocations.get(1);
									startPos = startPosition + step + 1;
									endPos = 17 - 1;
									for (int start = startPos; start <= endPos; start++) {
										bufferedValues.add(start, "Blank");
										++step;
									}
								} else if (savedNextLetter.get(nextIndex) == "U") {
									int startPosition = savedPreviousLocations.get(2);
									startPos = startPosition + step + 1;
									endPos = 20 - 1;
									for (int start = startPos; start <= endPos; start++) {
										bufferedValues.add(start, "Blank");
										++step;
									}
								} else if (savedNextLetter.get(nextIndex) == "AC") {
									int startPosition = savedPreviousLocations.get(3);
									startPos = startPosition + step + 1;
									endPos = 28 - 1;
									for (int start = startPos; start <= endPos; start++) {
										bufferedValues.add(start, "Blank");
									}
									step = 0;
								}
							}
						}
					}
				}
				if (bufferedValues.size() == headerColumnNames.size()) {
					itemCount++;
					arrayOfRecords.add(bufferedValues);
					UploadSettlementService.arrayList.add(bufferedValues);
					if (UploadSettlementService.arrayList.size() > 0) {
						totalCount = arrayList.size();
						int count = 2000;
						if (arrayOfRecords.size() % count == 0) {
							UploadData uploadData = new UploadData();
							uploadData.setPid(pid);
							uploadData.setFilename(originalFilename);
							uploadData.setItemCount(itemCount);
							uploadData.setProcessor(uploadType);
							uploadData.setData(arrayOfRecords);
							uploadData.setLast(isLast);
							sendRecordToServer(uploadData);	
//							
							UploadSettlementService.position = UploadSettlementService.arrayList.size();
						}
						bufferedValues = new ArrayList<String>(headerColumns);
					}
				}
			}
		}

		private void sendRecordToServer(UploadData uploadData) {
			try {
				Properties properties = CommonUtil.loadPropertySettings("settings");
				String baseUrl = properties.getProperty("base_url");
				String data = gson.toJson(uploadData);
				Post postData = Http.post(baseUrl + "api/v1/settlements/uploads", data)
						.header("Content-Type", "application/json").header("Authorization", authorization);
				System.out.println(postData.text());
				// reset
				arrayOfRecords = new ArrayList<List<String>>();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void characters(char[] ch, int start, int length) {
			lastContents += new String(ch, start, length);
		}
	}

//	public static void main(String[] args) throws Exception {
//		UploadSettlementService example = new UploadSettlementService();
//		// example.processOneSheet("C:\\Users\\borip\\Documents\\itex_ptsp.xlsx");
//		String[] headerColumnNames = { "Local_Date_Time", "Terminal_ID", "Merchant_ID", "Merchant_Name_Location",
//				"STAN", "PAN", "Message_Type", "From_Account_ID", "Merchant_Account_Nr", "Merchant_Account_Name",
//				"PTSP_Account_Nr", "From_Account_Type", "tran_type_description", "Response_Code_description",
//				"Tran_Amount_Req", "Tran_Amount_Rsp", "Surcharge", "Amount_Impact", "merch_cat_category_name",
//				"merch_cat_visa_category_name", "Settlement_Impact", "Settlement_Impact_Desc", "Merchant_Discount",
//				"Merchant_Receivable", "Auth_ID", "Tran_ID", "Retrieval_Reference_Nr", "Totals_Group", "Region",
//				"Transaction_Status1", "Transaction_Type_Impact", "Message_Type_Desc", "trxn_category" };
////		String[] headerColumnNames = { "TRANSACTION ID", "TRANSACTION TYPE", "TRANSACTION DATETIME", "SETTLEMENT DATE",
////				"APPROVAL CODE", "TRANNUMBER", "SIGN", "INVOICENUM", "ACQUIRERREFERENCENUMBER", "RETAILER ID",
////				"TERMINALID", "TERMINAL LOCATION", "MERCHANT DEPOSIT BANKNAME", "MERCHANT BANK ACCOUNT", "VENDORID",
////				"VENDORCODE", "VENDORNAME", "CARDSCHEME", "MASKEDPAN", "PHONE NO", "TRANAMOUNT", "TOTAL MSC RATE",
////				"LCY MSC VALUE", "SUBSIDY RATE", "TOTAL SUBSIDY VALUE", "LCY AMOUNT DUE MERCHANT", "UPHSS_TRANSREF",
////				"UPHSS", "REPORTTYPE" };
//		List<String> headerList = new ArrayList<String>();
//		for (int index = 0; index < headerColumnNames.length; index++) {
//			headerList.add(headerColumnNames[index]);
//		}
//		int headerRow = 1;
//		//example.processAllSheets("C:\\Users\\borip\\Documents\\upsl_sample_file.xlsx", headerList, headerRow);
//		example.processAllSheets("C:\\Users\\borip\\Documents\\ptsp_fee_file2.xlsx", headerList, headerRow);
//	}
}
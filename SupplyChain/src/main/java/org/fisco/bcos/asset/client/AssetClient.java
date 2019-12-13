package org.fisco.bcos.asset.client;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fisco.bcos.asset.contract.SupplyChain;
import org.fisco.bcos.asset.contract.SupplyChain.CreateEventEventResponse;
import org.fisco.bcos.asset.contract.SupplyChain.PassEventEventResponse;
import org.fisco.bcos.asset.contract.SupplyChain.BankEventEventResponse;
import org.fisco.bcos.asset.contract.SupplyChain.RedeemEventEventResponse;
import org.fisco.bcos.channel.client.Service;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.Keys;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.channel.ChannelEthereumService;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.tuples.generated.Tuple2;
import org.fisco.bcos.web3j.tx.gas.StaticGasProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class AssetClient {

	static Logger logger = LoggerFactory.getLogger(AssetClient.class);

	private Web3j web3j;

	private Credentials credentials;

	public Web3j getWeb3j() {
		return web3j;
	}

	public void setWeb3j(Web3j web3j) {
		this.web3j = web3j;
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public void recordAssetAddr(String address) throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		prop.setProperty("address", address);
		final Resource contractResource = new ClassPathResource("contract.properties");
		FileOutputStream fileOutputStream = new FileOutputStream(contractResource.getFile());
		prop.store(fileOutputStream, "contract address");
	}

	public String loadAssetAddr() throws Exception {
		// load SupplyChain contact address from contract.properties
		Properties prop = new Properties();
		final Resource contractResource = new ClassPathResource("contract.properties");
		prop.load(contractResource.getInputStream());

		String contractAddress = prop.getProperty("address");
		if (contractAddress == null || contractAddress.trim().equals("")) {
			throw new Exception(" load SupplyChain contract address failed, please deploy it first. ");
		}
		logger.info(" load SupplyChain address from contract.properties, address is {}", contractAddress);
		return contractAddress;
	}

	public void initialize() throws Exception {

		// init the Service
		@SuppressWarnings("resource")
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
		Service service = context.getBean(Service.class);
		service.run();

		ChannelEthereumService channelEthereumService = new ChannelEthereumService();
		channelEthereumService.setChannelService(service);
		Web3j web3j = Web3j.build(channelEthereumService, 1);

		// init Credentials
		Credentials credentials = Credentials.create(Keys.createEcKeyPair());

		setCredentials(credentials);
		setWeb3j(web3j);

		logger.debug(" web3j is " + web3j + " ,credentials is " + credentials);
	}

	private static BigInteger gasPrice = new BigInteger("30000000");
	private static BigInteger gasLimit = new BigInteger("30000000");

	public void deploySChainAndRecordAddr() {

		try {
			SupplyChain sChain = SupplyChain.deploy(web3j, credentials, new StaticGasProvider(gasPrice, gasLimit)).send();
			System.out.println(" deploy SupplyChain success, contract address is " + sChain.getContractAddress());

			recordAssetAddr(sChain.getContractAddress());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println(" deploy SupplyChain contract failed, error message is  " + e.getMessage());
		}
	}

	public void queryReceiptAmount(String fromAccount, String toAccount, String item) {
		try {
			String contractAddress = loadAssetAddr();

			SupplyChain sChain = SupplyChain.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			Tuple2<BigInteger, BigInteger> result = sChain.getReceipt(fromAccount, toAccount, item).send();
			if (result.getValue1().compareTo(new BigInteger("0")) == 0) {
				System.out.printf(" receipt from account %s to account %s for %s, value %s \n", fromAccount, toAccount, item, result.getValue2());
			} else {
				System.out.printf("receipt is not exist \n");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			logger.error(" queryReceiptAmount exception, error message is {}", e.getMessage());

			System.out.printf(" query receipt account failed, error message is %s\n", e.getMessage());
		}
	}

	public void createReceipt(String fromAccount, String toAccount, String item, BigInteger amount) {
		try {
			String contractAddress = loadAssetAddr();

			SupplyChain sChain = SupplyChain.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = sChain.createReceipt(fromAccount, toAccount, item, amount).send();
			List<CreateEventEventResponse> response = sChain.getCreateEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" create receipt success =>from: %s, to: %s, for: %s, value: %s \n", fromAccount,
							toAccount, item, amount);
				} else {
					System.out.printf(" create receipt failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" createReceipt exception, error message is {}", e.getMessage());
			System.out.printf(" create receipt failed, error message is %s\n", e.getMessage());
		}
	}

	public void passReceipt(String issuer, String from_account, String from_item, String to_account, String to_item, BigInteger amount) {
		try {
			String contractAddress = loadAssetAddr();
			SupplyChain sChain = SupplyChain.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = sChain.passReceipt(issuer, from_account, from_item, to_account, to_item, amount).send();
			List<PassEventEventResponse> response = sChain.getPassEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" pass receipt success => Receipt1: from: %s, to: %s, for: %s, Receipt2: from: %s, to: %s, for: %s, amount: %s \n",
							from_account, issuer, from_item, issuer, to_account, to_item, amount);
				} else {
					System.out.printf(" pass receipt failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" passReceipt exception, error message is {}", e.getMessage());
			System.out.printf(" pass receipt failed, error message is %s\n", e.getMessage());
		}
	}

	public void receiptToBank(String from_account, String to_account, String item) {
		try{
			String contractAddress = loadAssetAddr();
			SupplyChain sChain = SupplyChain.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = sChain.receiptToBank(from_account, to_account, item).send();
			List<BankEventEventResponse> response = sChain.getBankEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" receipt to bank success => Receipt: from: %s, to: %s, for: %s \n",
							from_account, to_account, item);
				} else {
					System.out.printf(" receipt to bank failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" receiptToBank exception, error message is {}", e.getMessage());
			System.out.printf(" receipt to bank failed, error message is %s\n", e.getMessage());
		}
	}

	public void redeemReceipt(String from_account, String to_account, String item) {
		try{
			String contractAddress = loadAssetAddr();
			SupplyChain sChain = SupplyChain.load(contractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
			TransactionReceipt receipt = sChain.redeemReceipt(from_account, to_account, item).send();
			List<RedeemEventEventResponse> response = sChain.getRedeemEventEvents(receipt);
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf(" redeem receipt success => Receipt: from: %s, to: %s, for: %s \n",
							from_account, to_account, item);
				} else {
					System.out.printf(" redeem receipt failed, ret code is %s \n",
							response.get(0).ret.toString());
				}
			} else {
				System.out.println(" event log not found, maybe transaction not exec. ");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			logger.error(" redeemReceipt exception, error message is {}", e.getMessage());
			System.out.printf(" redeem receipt failed, error message is %s\n", e.getMessage());
		}
	}

	public static void Usage() {
		System.out.println(" Usage:");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient deploy");
		System.out.println("\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient query fromAccount toAccount item");
		System.out.println(
				"\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient create fromAccount toAccount item amount");
		System.out.println(
				"\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient pass issuer from_account from_item to_account to_item amount");
		System.out.println(
				"\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient bank from_account to_account item");
		System.out.println(
				"\t java -cp conf/:lib/*:apps/* org.fisco.bcos.asset.client.AssetClient redeem from_account to_account item");
		System.exit(0);
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 1) {
			Usage();
		}

		AssetClient client = new AssetClient();
		client.initialize();

		switch (args[0]) {
		case "deploy":
			client.deploySChainAndRecordAddr();
			break;
		case "query":
			if (args.length < 4) {
				Usage();
			}
			client.queryReceiptAmount(args[1], args[2], args[3]);
			break;
		case "create":
			if (args.length < 5) {
				Usage();
			}
			client.createReceipt(args[1], args[2], args[3], new BigInteger(args[4]));
			break;
		case "pass":
			if (args.length < 7) {
				Usage();
			}
			client.passReceipt(args[1], args[2], args[3], args[4], args[5], new BigInteger(args[6]));
			break;
		case "bank":
			if (args.length < 4) {
				Usage();
			}
			client.receiptToBank(args[1], args[2], args[3]);
			break;
		case "redeem":
			if (args.length < 4) {
				Usage();
			}
			client.redeemReceipt(args[1], args[2], args[3]);
			break;
		default: {
			Usage();
		}
		}

		System.exit(0);
	}
}

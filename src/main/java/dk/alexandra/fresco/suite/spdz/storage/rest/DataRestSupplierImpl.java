/*******************************************************************************
 * Copyright (c) 2017 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.suite.spdz.storage.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import dk.alexandra.fresco.framework.MPCException;
import dk.alexandra.fresco.framework.Reporter;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzElement;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzInputMask;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzSInt;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import dk.alexandra.fresco.suite.spdz.storage.DataSupplier;

/**
 * Uses the gas station to fetch the next piece of preprocessed data.
 * @author Kasper Damgaard
 *
 */
public class DataRestSupplierImpl implements DataSupplier{

	//TODO: For now without security - but we need some kind of "login" or 
	//token based security such that only the parties with access can obtain the different parties shares.
	//Maybe use certificates and SSL connections instead, but this is harder to test and make work.

	private final static int tripleAmount = 10000;
	private final static int expAmount = 1000;
	private final static int bitAmount = 10000;
	private final static int inputAmount = 1000;

	private String restEndPoint;
	private int myId;

	private BigInteger modulus;
	private BigInteger alpha;

	private final BlockingQueue<SpdzTriple> triples;
	private final BlockingQueue<SpdzSInt> bits;
	private final BlockingQueue<SpdzSInt[]> exps;
	private final Map<Integer, BlockingQueue<SpdzInputMask>> inputs;
	
	private final List<RetrieverThread> threads = new ArrayList<>();

	public DataRestSupplierImpl(int myId, int noOfParties, String restEndPoint, int threadId) {		
		this.myId = myId;
		this.restEndPoint = restEndPoint;
		if(!this.restEndPoint.endsWith("/")) {
			this.restEndPoint += "/";
		}		
		this.restEndPoint += "api/fuel/";

		this.triples = new ArrayBlockingQueue<>(tripleAmount*3);
		this.bits = new ArrayBlockingQueue<>(bitAmount*10);
		this.exps= new ArrayBlockingQueue<>(expAmount*3);
		this.inputs = new HashMap<>();
		for(int i = 1; i <= noOfParties; i++) {
			this.inputs.put(i, new ArrayBlockingQueue<>(inputAmount*2));
		}

		//Start retriver threads
		for(Type t : Type.values()) {
			RetrieverThread thread = null;
			switch(t) {
			case TRIPLE:
				thread = new RetrieverThread(this.restEndPoint, myId, this, t, tripleAmount, threadId);
				thread.start();
				threads.add(thread);
				break;
			case BIT:
				thread = new RetrieverThread(this.restEndPoint, myId, this, t, bitAmount, threadId);
				thread.start();
				threads.add(thread);
				break;
			case EXP:
				thread = new RetrieverThread(this.restEndPoint, myId, this, t, expAmount, threadId);
				thread.start();
				threads.add(thread);
				break;
			case INPUT:
				for(int i = 1; i <= noOfParties; i++) {
					thread = new RetrieverThread(this.restEndPoint, myId, this, t, inputAmount, threadId, i);
					thread.start();
					threads.add(thread);
				}
				break;
			}
		}
	}	

	public void addTriples(SpdzTriple[] trips) throws InterruptedException {
		for(SpdzTriple t : trips) {
			this.triples.put(t);
		}
	}

	public void addExps(SpdzElement[][] exp) throws InterruptedException {
		for(SpdzElement[] es : exp) {
			SpdzSInt[] pipe = new SpdzSInt[es.length];
			int i = 0;
			for(SpdzElement elm : es) {
				pipe[i] = new SpdzSInt(elm);
				i++;
			}
			this.exps.put(pipe);
		}
	}

	public void addBits(SpdzElement[] bits) throws InterruptedException {
		for(SpdzElement elm : bits) {
			this.bits.put(new SpdzSInt(elm));
		}
	}

	public void addInputs(SpdzInputMask[] inps, int towardsId) throws InterruptedException {
		for(SpdzInputMask mask : inps) {
			this.inputs.get(towardsId).put(mask);
		}
	}

	@Override
	public SpdzTriple getNextTriple() {		
		try {
			return this.triples.take();
		} catch (InterruptedException e) {
			throw new MPCException("Supplier got interrupted before a new triple was made available", e);
		}
	}



	@Override
	public SpdzSInt[] getNextExpPipe() {
		try {
			return this.exps.take();
		} catch (InterruptedException e) {
			throw new MPCException("Supplier got interrupted before a new exp pipe was made available", e);
		}
	}

	@Override
	public SpdzInputMask getNextInputMask(int towardPlayerID) {
		try {
			return this.inputs.get(towardPlayerID).take();
		} catch (InterruptedException e) {
			throw new MPCException("Supplier got interrupted before a new triple was made available", e);
		}
	}

	@Override
	public SpdzSInt getNextBit() {
		try {
			return this.bits.take();
		} catch (InterruptedException e) {
			throw new MPCException("Supplier got interrupted before a new triple was made available", e);
		}
	}

	private BigInteger getBigInteger(String endpoint) {
		BigInteger result = null;
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {			
			HttpGet httpget = new HttpGet(this.restEndPoint + endpoint);

			Reporter.fine("Executing request " + httpget.getRequestLine());            

			// Create a custom response handler
			ResponseHandler<BigInteger> responseHandler = new ResponseHandler<BigInteger>() {

				@Override
				public BigInteger handleResponse(
						final HttpResponse response) throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {                        
						BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
						StringBuffer result = new StringBuffer();
						String line = "";
						while ((line = rd.readLine()) != null) {
							result.append(line);
						}
						return new BigInteger(result.toString());
					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}

			};
			result = httpClient.execute(httpget, responseHandler);       
		} catch (ClientProtocolException e) {
			throw new MPCException("Could not complete the http request", e);
		} catch (IOException e) {
			throw new MPCException("IO error", e);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				//silent crashing - nothing to do at this point. 
			}        	
		}
		return result;
	}

	@Override
	public BigInteger getModulus() {
		if(this.modulus == null) {
			this.modulus = this.getBigInteger("modulus");
		}
		return this.modulus;
	}

	@Override
	public BigInteger getSSK() {
		if(this.alpha == null) {
			this.alpha = this.getBigInteger("alpha/"+this.myId);
		}
		return alpha;
	}

	@Override
	public SpdzSInt getNextRandomFieldElement() {
		return new SpdzSInt(this.getNextTriple().getA());
	}

	@Override
	public void shutdown() {
		for(RetrieverThread t : threads) {
			t.stopThread();
			t.interrupt();			
		}
	}



}

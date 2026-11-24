package il.ac.tau.cs.software1.inventory;

/*
 * A simple array-based Inventory (Question 6).
 */
public class NaiveInventory implements Inventory {

	private static final int INITIAL_CAPACITY = 100;
	private Collectible[] mCollectibles;
	private int actualCount;

	public NaiveInventory() {
		mCollectibles = new Collectible[INITIAL_CAPACITY];
		actualCount = 0;
	}

	@Override
	public void collect(Collectible collectible) {
		if (actualCount >= mCollectibles.length) { // resize the array
			Collectible[] newArray = new Collectible[mCollectibles.length * 2];
			for (int i = 0; i < mCollectibles.length; i++) {
				newArray[i] = mCollectibles[i];
			}
			mCollectibles = newArray;
		}
		mCollectibles[actualCount++] = collectible; // add the collectible to the array and increment the count
	}

	@Override
	public float getTotalWeight() {
		float totalWeight = 0; // initialize total weight to 0
		for (int i = 0; i < actualCount; i++) { // count only the actual collectibles, not the entire array
			totalWeight += mCollectibles[i].getWeight();
		}
		return totalWeight;
	}

	@Override
	public int getCurrentCount() {
		return actualCount;
	}

	// Returns the array of Collectibles that is stored in the field of this class
	public Collectible[] getCollArr() {
		return mCollectibles;
	}
}

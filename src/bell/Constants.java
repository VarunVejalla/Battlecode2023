package bell;

public final class Constants {

    private Constants(){
        // restrict instantiation
    }



    public static final int FULL_MASK = 65535; // 11111111111111



    //HQ Stuff
    public static final int HQ0_LOC_IDX = 0;
    public static final int HQ1_LOC_IDX = 1;
    public static final int HQ2_LOC_IDX = 2;
    public static final int HQ3_LOC_IDX = 3;
    public static final int[] HQ_LOC_IDX_MAP = {HQ0_LOC_IDX, HQ1_LOC_IDX, HQ2_LOC_IDX, HQ3_LOC_IDX};

    public static final int HQ_X_MASK = 4032;
    public static final int HQ_X_SHIFT = 6;
    public static final int HQ_Y_MASK = 63;
    public static final int HQ_Y_SHIFT = 0;

    public static final int HQ0_RESOURCES_IDX = 4;
    public static final int HQ1_RESOURCES_IDX = 5;
    public static final int HQ2_RESOURCES_IDX = 6;
    public static final int HQ3_RESOURCES_IDX = 7;
    public static final int[] HQ_RESOURCES_IDX_MAP = {HQ0_RESOURCES_IDX, HQ1_RESOURCES_IDX, HQ2_RESOURCES_IDX, HQ3_RESOURCES_IDX};

    public static final int HQ_MANA_MASK = 127; // 0000000001111111 (digits 0-6)
    public static final int HQ_MANA_SHIFT = 0;
    public static final int HQ_ADAMANTIUM_MASK = 16256; // 0011111110000000 (digits 7-13)
    public static final int HQ_ADAMANTIUM_SHIFT = 7;




    // Region consts

    public static final int NUM_REGIONS_HORIZONTAL = 7;
    public static final int NUM_REGIONS_VERTICAL = 7;
    public static final int NUM_REGIONS_TOTAL = NUM_REGIONS_HORIZONTAL * NUM_REGIONS_VERTICAL;

    public static final int REGION_START_IDX = 43;
    public static final int REGIONS_PER_COMM = 5;
    public static final int REGION_MASK_SIZE = 3;



    // Island consts
    public static final int ISLAND_START_IDX = 8;
    public static  final int ISLAND_X_MASK = 4032;
    public static final int ISLAND_X_SHIFT = 6;
    public static final int ISLAND_Y_MASK = 63;
    public static final int ISLAND_Y_SHIFT = 0;
    public static final int ISLAND_CONTROL_MASK = 12288;
    public static final int ISLAND_CONTROL_SHIFT = 12;

}

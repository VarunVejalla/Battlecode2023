def encode(x, y):
    y *= -1
    y += 4
    x += 4
    return y*7 + x

def make_grid():
    """for visualization"""
    grid = []
    for dy in range(-3, 4):
        row = []
        for dx in range(-3,4):
            if dx**2 + dy** 2 <= 9:
                row.append(encode(dx,dy))
            else:
                row.append("*")
        grid.append(row)
    return grid


encoded_to_coordinates = {}
coordinates_to_encoded = {}
coordinates = []
direction_to_coordinate = {"North":(0,1), "South":(0,-1), "East":(1,0), "West":(-1,0), "NorthEast":(1,1), "NorthWest":(-1, 1), "SouthWest":(-1, -1), "SouthEast":(1,-1)}


def generate_dictionaries():
    for dy in range(-3, 4):
        for dx in range(-3,4):
            if dx**2 + dy** 2 <= 9:
                encoding = encode(dx, dy)
                coordinate = (dx, dy)
                encoded_to_coordinates[encoding] = (dx, dy)
                coordinates_to_encoded[coordinate] = encoding
                coordinates.append(coordinate)


def generate_method(direction):
    dx, dy = direction_to_coordinate[direction]
    desired_x, desired_y = dx*3, dy*3
    coordinates.sort(key=lambda coordinate: (coordinate[0]-desired_x)**2 + (coordinate[1]-desired_y)**2)
    sorted_encodings = [coordinates_to_encoded[coord] for coord in coordinates]
    
    header = f"public boolean trySpawn{direction}(RobotType type) throws GameActionException {{"
    body = """\n int currX = robot.myLoc.x;    \n int currY = robot.myLoc.y; \n\n"""

    for (dx,dy), encoding in list(zip(coordinates, sorted_encodings)):
        if dx >= 0:
            dx = "+" + str(dx)
        if dy >= 0:
            dy = "+" + str(dy)
        statement = f"""MapLocation l{encoding} = new MapLocation(currX {dx}, currY {dy});  // ({dx}, {dy}) relative to center
        if(rc.canBuildRobot(type, l{encoding})){{
            rc.buildRobot(type, l{encoding});
            return true;
            }} \n"""
        body += statement
    body += "return false; \n }\n\n"
    return header + body 


def generate_file(output_file):
    generate_dictionaries()
    header = """import battlecode.common.*;
    
    public class Spawner{
        RobotController rc;
        Robot robot;


        public Spawner(RobotController rc, Robot robot){
            this.rc = rc;
            this.robot = robot;
        }

        // see this link for a visualization of the map locations with the encodings used her: https://docs.google.com/spreadsheets/d/1JZBAelcHcSx7x9B9q2wqjYOSKeEJ-bods9M-qlyBCnw/edit?usp=sharing

        public boolean trySpawnGeneralDirection(RobotType type, Direction spawnDir) throws GameActionException{
            switch(spawnDir){
                case NORTH: return trySpawnNorth(type);
                case SOUTH: return trySpawnSouth(type);
                case EAST: return trySpawnEast(type);
                case WEST: return trySpawnWest(type);
                case NORTHEAST: return trySpawnNorthEast(type);
                case NORTHWEST: return trySpawnNorthWest(type);
                case SOUTHEAST: return trySpawnSouthEast(type);
                case SOUTHWEST: return trySpawnSouthWest(type);
                default: return false;
            }
        }\n \n"""

    body = ""
    for direction in direction_to_coordinate.keys():
        body += generate_method(direction)
    body += "} \n \n"

    with open(output_file, "w") as f:
        f.write(header + body)

if __name__ == "__main__":
    generate_file("spawnDirs.txt")
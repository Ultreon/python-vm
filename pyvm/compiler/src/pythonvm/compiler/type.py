class Type:
    def __init__(self, name: str):
        self.name = name
        self.__element_type = None

        if name[0] == "[":
            cur = name[1:]
            while cur[0] == "[":
                cur = cur[1:]
            self.__element_type = Type(cur)

        if name[0] == "L":
            if name[-1] != ";":
                raise ValueError("Invalid class name")
            self.name = name[1:-1]

    @staticmethod
    def from_class_name(class_name: str):
        return Type(f"L{class_name.replace(".", "/")};")

    @staticmethod
    def from_internal_name(internal_name: str):
        return Type(f"L{internal_name};")

    @staticmethod
    def from_type(type_: "Type"):
        return Type(type_.name)

    @property
    def is_primitive(self):
        return self.sort in ["Z", "B", "C", "S", "I", "J", "F", "D"]

    @property
    def sort(self):
        return self.name[0]

    @property
    def is_array(self):
        return self.sort == "["

    @property
    def is_object(self):
        return self.sort == "L" and self.name[-1] == ";"

    @property
    def element_type(self):
        return self.__element_type

    @property
    def internal_name(self):
        if self.is_object:
            return self.name[1:-1]
        else:
            return None

    @property
    def class_name(self):
        if self.is_object:
            return self.name[1:-1].replace("/", ".")
        elif self.is_array:
            return f"{self.element_type.class_name}[]"
        else:
            match self.sort:
                case "Z":
                    return "boolean"
                case "B":
                    return "byte"
                case "C":
                    return "char"
                case "S":
                    return "short"
                case "I":
                    return "int"
                case "J":
                    return "long"
                case "F":
                    return "float"
                case "D":
                    return "double"
                case "V":
                    return "void"
            return None

    @property
    def is_void(self):
        return self.name == "V"

    @property
    def size(self):
        match self.sort:
            case "[", "Z", "B", "C", "S", "I", "F":
                return 1
            case "[", "J", "D":
                return 2
            case _:
                return 0

    def __str__(self):
        return self.name

    def __repr__(self):
        return f"<Type {self.name}>"


VOID_TYPE = Type("V")
BOOLEAN_TYPE = Type("Z")
BYTE_TYPE = Type("B")
SHORT_TYPE = Type("S")
CHAR_TYPE = Type("C")
INT_TYPE = Type("I")
LONG_TYPE = Type("J")
FLOAT_TYPE = Type("F")
DOUBLE_TYPE = Type("D")
OBJECT_TYPE = Type("Ljava/lang/Object;")

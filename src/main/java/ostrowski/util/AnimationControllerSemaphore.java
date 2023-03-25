package ostrowski.util;

public class AnimationControllerSemaphore
{
   public static final int CLASS_GLVIEW_MESSAGES                     = Semaphore.DependsOn(0);
   public static final int CLASS_OBJHEX_TEXTUREDOBJECTS              = Semaphore.DependsOn(0);
   public static final int CLASS_OBJHEX_OBJECTS                      = Semaphore.DependsOn(0);

   public static final int CLASS_BODYPART_CHILDREN_HEAD              = Semaphore.DependsOn(0);
   public static final int CLASS_BODYPART_CHILDREN_TOES              = Semaphore.DependsOn(0);
   public static final int CLASS_BODYPART_CHILDREN_WEAPON            = Semaphore.DependsOn(0);
// order 2:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_BODYPART_CHILDREN_FEET              = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_TOES);
   public static final int CLASS_BODYPART_CHILDREN_FINGERS           = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_WEAPON);
   public static final int CLASS_BODYPART_CHILDREN_LOWERLEG          = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_FEET);
// order 3:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_BODYPART_CHILDREN_LEGS              = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_LOWERLEG);
   public static final int CLASS_BODYPART_CHILDREN_HANDS             = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_FINGERS);
// order 4:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_BODYPART_CHILDREN_PELVIS            = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_LEGS);
   public static final int CLASS_BODYPART_CHILDREN_FOREARM           = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_HANDS);
// order 5:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_BODYPART_CHILDREN_ARMS              = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_FOREARM);
// order 6:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_BODYPART_CHILDREN_TORSO             = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_HEAD,
                                                                                           CLASS_BODYPART_CHILDREN_ARMS);
// order 7:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_BODYPART_CHILDREN_BODY              = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_TORSO,
                                                                                           CLASS_BODYPART_CHILDREN_PELVIS);
   
// order 8:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_OBJHEX_HUMANS                       = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_BODY);
// order 9:-------------------------------------------------------------------------------------------------------------------------------------
   public static final int CLASS_GLVIEW_MODELS                       = Semaphore.DependsOn(CLASS_BODYPART_CHILDREN_BODY,
                                                                                           CLASS_OBJHEX_HUMANS);


}
